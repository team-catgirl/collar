package team.catgirl.collar.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import spark.Request;
import team.catgirl.collar.server.common.ServerVersion;
import team.catgirl.collar.server.http.AuthToken;
import team.catgirl.collar.server.http.HttpException;
import team.catgirl.collar.server.http.HttpException.UnauthorisedException;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.http.SessionManager;
import team.catgirl.collar.server.mongo.Mongo;
import team.catgirl.collar.server.security.ServerIdentityStore;
import team.catgirl.collar.server.security.hashing.PasswordHashing;
import team.catgirl.collar.server.security.signal.SignalServerIdentityStore;
import team.catgirl.collar.server.services.authentication.AuthenticationService;
import team.catgirl.collar.server.services.authentication.AuthenticationService.CreateAccountRequest;
import team.catgirl.collar.server.services.authentication.AuthenticationService.LoginRequest;
import team.catgirl.collar.server.services.authentication.TokenCrypter;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.services.profiles.ProfileService.GetProfileRequest;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static spark.Spark.*;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String portValue = System.getenv("PORT");
        if (portValue != null) {
            port(Integer.parseInt(portValue));
        } else {
            port(3000);
        }

        LOGGER.info("Reticulating splines...");

        // Services
        MongoDatabase db = Mongo.database();

        ObjectMapper mapper = Utils.createObjectMapper();
        SessionManager sessions = new SessionManager(mapper);
        ServerIdentityStore serverIdentityStore = new SignalServerIdentityStore(db);
        ProfileService profiles = new ProfileService(db);
        // TODO: pass this in as configuration
        TokenCrypter tokenCrypter = new TokenCrypter("mycoolpassword");
        PasswordHashing passwordHashing = new PasswordHashing();
        AuthenticationService auth = new AuthenticationService(profiles, passwordHashing, tokenCrypter);

        // Collar feature services
        GroupService groups = new GroupService(serverIdentityStore.getIdentity(), sessions);

        // Always serialize objects returned as JSON
        defaultResponseTransformer(mapper::writeValueAsString);
        exception(HttpException.class, (e, request, response) -> {
            response.status(e.code);
            response.body(e.getMessage());
        });

        // Setup WebSockets
        webSocketIdleTimeoutMillis((int) TimeUnit.SECONDS.toMillis(60));

        // WebSocket server
        webSocket("/api/1/listen", new Collar(mapper, sessions, groups, serverIdentityStore));

        // API routes
        path("/api", () -> {
            // Version 1
            path("/1", () -> {

                before("/*", (request, response) -> {
                    setupRequest(tokenCrypter, request);
                });

                // Used to test if API is available
                get("/", (request, response) -> new ServerStatusResponse("OK"));

                path("/profile", () -> {
                    before("/*", (request, response) -> {
                        RequestContext.from(request).assertIsUser();
                    });
                    // Get your own profile
                    get("/me", (request, response) -> {
                        RequestContext context = RequestContext.from(request);
                        return profiles.getProfile(context, GetProfileRequest.byId(context.profileId)).profile;
                    });
                    // Get someone elses profile
                    get("/:id", (request, response) -> {
                        String id = request.params("id");
                        UUID uuid = UUID.fromString(id);
                        return profiles.getProfile(RequestContext.from(request), GetProfileRequest.byId(uuid)).profile.toPublic();
                    });
                });

                path("/auth", () -> {
                    before("/*", (request, response) -> {
                        RequestContext.from(request).assertAnonymous();
                    });
                    // Login
                    get("/login", (request, response) -> {
                        LoginRequest req = mapper.readValue(request.bodyAsBytes(), LoginRequest.class);
                        return auth.login(RequestContext.from(request), req);
                    });
                    // Create an account
                    get("/create", (request, response) -> {
                        CreateAccountRequest req = mapper.readValue(request.bodyAsBytes(), CreateAccountRequest.class);
                        return auth.createAccount(RequestContext.from(request), req);
                    });
                });
            });
        });

        // Reports server version
        // This contract is forever, please change with care!
        get("/api/version", (request, response) -> ServerVersion.version());
        // Query this route to discover what version of the APIs are supported
        get("/api/discover", (request, response) -> {
            List<Integer> apiVersions = new ArrayList<>();
            apiVersions.add(1);
            return apiVersions;
        });
        // Return nothing
        get("/", (request, response) -> "", Object::toString);

        LOGGER.info("Collar server started. Do you want to play a block game game?");
    }

    /**
     * @param request http request
     * @throws IOException on token decoding
     */
    private static void setupRequest(TokenCrypter crypter, Request request) throws IOException {
        String authorization = request.headers("Authorization");
        RequestContext context;
        if (authorization == null) {
            context = RequestContext.ANON;
        } else if (authorization.startsWith("Bearer ")) {
            String tokenString = authorization.substring(authorization.lastIndexOf(" "));
            AuthToken token = AuthToken.deserialize(crypter, tokenString);
            context = token.fromToken();
        } else {
            throw new UnauthorisedException("bad authorization header");
        }
        request.attribute("requestContext", context);
    }

    private static class ServerStatusResponse {
        @JsonProperty("status")
        public final String status;

        public ServerStatusResponse(@JsonProperty("status") String status) {
            this.status = status;
        }
    }
}
