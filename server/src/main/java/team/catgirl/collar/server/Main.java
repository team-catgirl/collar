package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.common.ServerVersion;
import team.catgirl.collar.server.mongo.Mongo;
import team.catgirl.collar.server.security.ServerIdentityStore;
import team.catgirl.collar.server.security.signal.SignalServerIdentityStore;
import team.catgirl.collar.utils.Utils;
import team.catgirl.collar.server.http.HttpException;
import team.catgirl.collar.server.managers.GroupManager;
import team.catgirl.collar.server.managers.SessionManager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
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

        // Services
        MongoDatabase db = Mongo.database();

        ObjectMapper mapper = Utils.createObjectMapper();
        SessionManager sessions = new SessionManager(mapper);
        ServerIdentityStore serverIdentityStore = new SignalServerIdentityStore(db);
        ServerIdentity identity = serverIdentityStore.getIdentity();
        GroupManager groups = new GroupManager(identity, sessions);

        // Always serialize objects returned as JSON
        defaultResponseTransformer(mapper::writeValueAsString);
        exception(HttpException.class, (e, request, response) -> {
            response.status(e.code);
            response.body(e.getMessage());
        });

        // Setup WebSockets
        webSocketIdleTimeoutMillis((int) TimeUnit.SECONDS.toMillis(60));
        webSocket("/api/1/listen", new Collar(mapper, sessions, groups, serverIdentityStore));

        // Version routes
        path("/api/1", () -> {
            // Used to test if API is available
            get("/", (request, response) -> new ServerStatusResponse("OK"));
            // The servers current identity
            get("/identity", (request, response) -> identity);
        });

        // Server routes

        // Reports server version
        get("/api/version", (request, response) -> ServerVersion.version());
        // Query this route to discover what version of the APIs are supported
        get("/api/version/discover", (request, response) -> {
            List<ServerVersion> apiVersions = new ArrayList<>();
            apiVersions.add(new ServerVersion(1, 0, 0));
            return apiVersions;
        });
        // Return nothing
        get("/", (request, response) -> "", Object::toString);
    }

    private static class ServerStatusResponse {
        public final String status;

        public ServerStatusResponse(String status) {
            this.status = status;
        }
    }
}
