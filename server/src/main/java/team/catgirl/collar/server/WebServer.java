package team.catgirl.collar.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.GroupType;
import team.catgirl.collar.api.groups.MembershipRole;
import team.catgirl.collar.api.http.*;
import team.catgirl.collar.api.http.HttpException.*;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.profiles.Role;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.server.common.ServerVersion;
import team.catgirl.collar.server.configuration.Configuration;
import team.catgirl.collar.server.http.ApiToken;
import team.catgirl.collar.server.http.Cookie;
import team.catgirl.collar.server.http.HandlebarsTemplateEngine;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.api.authentication.AuthenticationService.*;
import team.catgirl.collar.server.services.authentication.TokenCrypter;
import team.catgirl.collar.server.services.devices.DeviceService;
import team.catgirl.collar.server.services.devices.DeviceService.DeleteDeviceRequest;
import team.catgirl.collar.server.services.devices.DeviceService.TrustDeviceResponse;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.api.profiles.ProfileService.GetProfileRequest;
import team.catgirl.collar.api.profiles.ProfileService.UpdateProfileRequest;
import team.catgirl.collar.server.services.textures.TextureService;
import team.catgirl.collar.server.services.textures.TextureService.*;
import team.catgirl.collar.utils.Utils;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class WebServer {

    private static final HandlebarsTemplateEngine TEMPLATE_ENGINE = new HandlebarsTemplateEngine("/templates");
    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());

    private final Configuration configuration;

    public WebServer(Configuration configuration) {
        this.configuration = configuration;
    }

    public void start(Consumer<Services> callback) {
        LOGGER.info("Reticulating splines...");
        // Set http port
        port(configuration.httpPort);
        // Services
        Services services = new Services(configuration);

        // Always serialize objects returned as JSON
        exception(HttpException.class, (e, request, response) -> {
            response.status(e.code);
            try {
                response.body(Utils.jsonMapper().writeValueAsString(new ErrorResponse(e.getMessage())));
            } catch (JsonProcessingException jsonProcessingException) {
                throw new RuntimeException(e);
            }
            LOGGER.log(Level.SEVERE, request.pathInfo(), e);
        });

        exception(Exception.class, (e, request, response) -> {
            response.status(500);
            try {
                response.body(Utils.jsonMapper().writeValueAsString(new ErrorResponse(e.getMessage())));
            } catch (JsonProcessingException jsonProcessingException) {
                throw new RuntimeException(e);
            }
            LOGGER.log(Level.SEVERE, request.pathInfo(), e);
        });

        // TODO: all routes should go into their own class/package
        // Setup WebSockets
        webSocketIdleTimeoutMillis((int) TimeUnit.SECONDS.toMillis(60));

        // TODO: all routes should go into their own class/package
        // Setup WebSockets
        webSocketIdleTimeoutMillis((int) TimeUnit.SECONDS.toMillis(60));
        webSocket("/api/1/listen", new CollarServer(services));

        staticFiles.location("/public");

        // Setup CORS
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            response.header("Access-Control-Allow-Credentials", "true");
            return "OK";
        }, Object::toString);

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Credentials", "true");
        });

        // API routes
        path("/api", () -> {

            // Version 1
            path("/1", () -> {

                before("/*", (request, response) -> {
                    setupRequest(services.tokenCrypter, request);
                });

                // Used to test if API is available
                get("/", (request, response) -> new ServerStatusResponse("OK"), services.jsonMapper::writeValueAsString);

                path("/profile", () -> {
                    before("/*", (request, response) -> {
                        assertAuthenticated(request);
                    });
                    get("/", (request, response) -> {
                        String email = request.queryParams("email");
                        RequestContext context = from(request);
                        return services.profiles.getProfile(context, GetProfileRequest.byEmail(email));
                    });
                    // Get your own profile
                    get("/me", (request, response) -> {
                        RequestContext context = from(request);
                        return services.profiles.getProfile(context, GetProfileRequest.byId(context.owner)).profile;
                    }, services.jsonMapper::writeValueAsString);
                    // Get someone elses profile
                    get("/:id", (request, response) -> {
                        String id = request.params("id");
                        UUID uuid = UUID.fromString(id);
                        return services.profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(uuid)).profile.toPublic();
                    }, services.jsonMapper::writeValueAsString);
                    get("/groups", (request, response) -> {
                        RequestContext context = from(request);
                        return services.groupStore.findGroupsContaining(new Player(context.owner, null)).collect(Collectors.toList());
                    }, services.jsonMapper::writeValueAsString);
                    post("/reset", (request, response) -> {
                        LoginRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), LoginRequest.class);
                        RequestContext context = from(request);
                        LoginResponse loginResp = services.auth.login(context, req);
                        if (!context.hasRole(Role.ADMINISTRATOR) && !context.callerIs(loginResp.profile.id)) {
                            throw new BadRequestException("user mismatch");
                        }
                        services.profileStorage.delete(context.owner);
                        services.profiles.updateProfile(context, UpdateProfileRequest.privateIdentityToken(loginResp.profile.id, new byte[0]));
                        return new Object();
                    }, services.jsonMapper::writeValueAsString);
                    get("/devices", (request, response) -> {
                        return services.devices.findDevices(from(request), services.jsonMapper.readValue(request.bodyAsBytes(), DeviceService.FindDevicesRequest.class));
                    }, services.jsonMapper::writeValueAsString);
                    post("/devices/trust", (request, response) -> {
                        RequestContext context = from(request);
                        context.assertNotAnonymous();
                        DeviceService.TrustDeviceRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), DeviceService.TrustDeviceRequest.class);
                        DeviceService.CreateDeviceResponse device = services.devices.createDevice(context, new DeviceService.CreateDeviceRequest(context.owner, req.deviceName));
                        PublicProfile profile = services.profiles.getProfile(context, GetProfileRequest.byId(context.owner)).profile.toPublic();
                        services.sessions.onDeviceRegistered(services.identityStore.getIdentity(), profile, req.token, device);
                        return new TrustDeviceResponse();
                    }, services.jsonMapper::writeValueAsString);
                    delete("/devices/:id", (request, response) -> {
                        RequestContext context = from(request);
                        String deviceId = request.params("id");
                        return services.devices.deleteDevice(context, new DeleteDeviceRequest(context.owner, Integer.parseInt(deviceId)));
                    });
                    get("/textures/:type", (request, response) -> {
                        RequestContext context = from(request);
                        TextureType textureType = TextureType.valueOf(request.params("type").toUpperCase());
                        PublicProfile profile = services.profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(context.owner)).profile.toPublic();
                        // Find all textures in groups
                        List<TextureService.Texture> groupTextures = services.groupStore.findGroupsContaining(profile)
                                .filter(group -> group.type == GroupType.GROUP)
                                .flatMap(group -> services.textures.findTextures(context, new FindTexturesRequest(textureType, null, group.id)).textures.stream())
                                .collect(Collectors.toList());
                        // Find all the textures belonging to player
                        List<TextureService.Texture> playerTextures = services.textures.findTextures(from(request), new FindTexturesRequest(textureType, context.owner, null)).textures;
                        return new FindTexturesResponse(ImmutableList.<TextureService.Texture>builder().addAll(playerTextures).addAll(groupTextures).build());
                    }, services.jsonMapper::writeValueAsString);
                    post("/textures/upload", (request, response) -> {
                        RequestContext context = from(request);
                        CreateTextureRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), CreateTextureRequest.class);
                        if (req.group != null) {
                            Group group = services.groups.findGroup(req.group).orElseThrow(() -> new NotFoundException("could not find group " + req.group));
                            if (group.members.stream().noneMatch(member -> member.player.profile.equals(context.owner) && member.membershipRole == MembershipRole.OWNER)) {
                                throw new NotFoundException("could not find group " + req.group);
                            }
                        }
                        return services.textures.createTexture(context, req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/textures/:type/default", (request, response) -> {
                        RequestContext context = from(request);
                        UpdateProfileRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), UpdateProfileRequest.class);
                        context.assertCallerIs(req.profile);
                        if (req.cape != null) {
                            throw new HttpException.BadRequestException("missing capeTexture");
                        }
                        return services.profiles.updateProfile(context, req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/reset", (request, response) -> {
                        RequestContext context = from(request);
                        services.profileStorage.delete(context.owner);
                        services.profiles.updateProfile(context, UpdateProfileRequest.privateIdentityToken(context.owner, new byte[0]));
                        response.status(204);
                        return null;
                    }, services.jsonMapper::writeValueAsString);
                });

                path("/auth", () -> {
                    before("/*", (request, response) -> {
                        from(request).assertAnonymous();
                    });
                    // Login
                    post("/login", (request, response) -> {
                        LoginRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), LoginRequest.class);
                        return services.auth.login(from(request), req);
                    }, services.jsonMapper::writeValueAsString);
                    // Create an account
                    post("/create", (request, response) -> {
                        CreateAccountRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), CreateAccountRequest.class);
                        return services.auth.createAccount(from(request), req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/verify", (request, response) -> {
                        VerifyAccountRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), VerifyAccountRequest.class);
                        return services.auth.verify(from(request), req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/reset/request", (request, response) -> {
                        RequestPasswordResetRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), RequestPasswordResetRequest.class);
                        return services.auth.requestPasswordReset(from(request), req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/reset/perform", (request, response) -> {
                        ResetPasswordRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), ResetPasswordRequest.class);
                        return services.auth.resetPassword(from(request), req);
                    });
                });

                get("/textures/:id/png", (request, response) -> {
                    String idAsString = request.params("id");
                    UUID uuid = UUID.fromString(idAsString);
                    byte[] bytes = services.textures.getTextureContent(new GetTextureContentRequest(uuid)).content.bytes;
                    response.header("Content-Type", "image/png");
                    try (ServletOutputStream outputStream = response.raw().getOutputStream()) {
                        outputStream.write(bytes);
                    }
                    return "";
                }, services.jsonMapper::writeValueAsString);
            });
        });

        // Reports server version
        // This contract is forever, please change with care!
        get("/api/version", (request, response) -> ServerVersion.version(), services.jsonMapper::writeValueAsString);
        // Query this route to discover what version of the APIs are supported and how the server is configured
        get("/api/discover", (request, response) -> {
            List<CollarVersion> versions = new ArrayList<>();
            versions.add(new CollarVersion(0, 1));
            List<CollarFeature> features = new ArrayList<>();
            features.add(new CollarFeature("auth:verification_scheme", configuration.minecraftSessionVerifier.getName()));
            features.add(new CollarFeature("groups:locations", true));
            features.add(new CollarFeature("groups:waypoints", true));
            features.add(new CollarFeature("profile:friends", true));
            return new DiscoverResponse(versions, features);
        }, services.jsonMapper::writeValueAsString);

        callback.accept(services);
        LOGGER.info("Collar server started.");
        LOGGER.info(services.urlProvider.homeUrl());
    }

    public static RequestContext from(Request req) {
        return req.attribute("requestContext");
    }

    private void assertAuthenticated(Request request) {
        // Must let in OPTIONS because CORS is horrible
        if (!request.requestMethod().equals("OPTIONS")) {
            from(request).assertNotAnonymous();
        }
    }

    private void setLoginCookie(Services services, Response response, Profile profile) throws IOException {
        Cookie cookie = new Cookie(profile.id, new Date().getTime() + TimeUnit.DAYS.toMillis(1));
        cookie.set(services.tokenCrypter, response);
    }

    private void setErrorResponse(Exception e, Response response) {
        try {
            response.body(Utils.jsonMapper().writeValueAsString(new ErrorResponse(e.getMessage())));
        } catch (JsonProcessingException jsonProcessingException) {
            LOGGER.log(Level.SEVERE, "Could not create error response ", e);
        }
    }

    private static String render(Map<String, Object> context, String templatePath) {
        return TEMPLATE_ENGINE.render(new ModelAndView(context, templatePath));
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
            String tokenString = authorization.substring(authorization.indexOf(" ") + 1);
            ApiToken token = ApiToken.deserialize(crypter, tokenString);
            if (token.isExpired()) {
                throw new UnauthorisedException("expired token");
            }
            context = token.fromToken();
        } else {
            throw new UnauthorisedException("bad authorization header");
        }
        request.attribute("requestContext", context);
    }

    public static final class ErrorResponse {
        public final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
}
