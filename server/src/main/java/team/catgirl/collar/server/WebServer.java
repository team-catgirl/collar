package team.catgirl.collar.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import team.catgirl.collar.api.http.*;
import team.catgirl.collar.api.http.HttpException.ForbiddenException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.api.http.HttpException.UnauthorisedException;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.server.common.ServerVersion;
import team.catgirl.collar.server.configuration.Configuration;
import team.catgirl.collar.server.http.ApiToken;
import team.catgirl.collar.server.http.Cookie;
import team.catgirl.collar.server.http.HandlebarsTemplateEngine;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.protocol.GroupsProtocolHandler;
import team.catgirl.collar.server.protocol.LocationProtocolHandler;
import team.catgirl.collar.server.protocol.ProtocolHandler;
import team.catgirl.collar.server.protocol.TexturesProtocolHandler;
import team.catgirl.collar.server.services.authentication.AuthenticationService.*;
import team.catgirl.collar.server.services.authentication.TokenCrypter;
import team.catgirl.collar.server.services.authentication.VerificationToken;
import team.catgirl.collar.server.services.devices.DeviceService;
import team.catgirl.collar.server.services.devices.DeviceService.TrustDeviceResponse;
import team.catgirl.collar.server.services.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileService.GetProfileRequest;
import team.catgirl.collar.server.services.textures.TextureService.GetTextureContentRequest;
import team.catgirl.collar.utils.Utils;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        defaultResponseTransformer(services.jsonMapper::writeValueAsString);
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

        // WebSocket server
        List<ProtocolHandler> protocolHandlers = new ArrayList<>();
        protocolHandlers.add(new GroupsProtocolHandler(services.groups));
        protocolHandlers.add(new LocationProtocolHandler(services.playerLocations));
        protocolHandlers.add(new TexturesProtocolHandler(services.identityStore.getIdentity(), services.sessions, services.textures));

        // TODO: all routes should go into their own class/package
        // Setup WebSockets
        webSocketIdleTimeoutMillis((int) TimeUnit.SECONDS.toMillis(60));
        webSocket("/api/1/listen", new CollarServer(services, protocolHandlers));

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

            return "OK";
        }, Object::toString);

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
        });

        webSocket("/api/1/listen", new CollarServer(services, protocolHandlers));

        // API routes
        path("/api", () -> {

            // Version 1
            path("/1", () -> {

                before("/*", (request, response) -> {
                    setupRequest(services.tokenCrypter, request);
                });

                // Used to test if API is available
                get("/", (request, response) -> new ServerStatusResponse("OK"));

                path("/profile", () -> {
                    before("/*", (request, response) -> {
                        RequestContext.from(request).assertNotAnonymous();
                    });
                    // Get your own profile
                    get("/me", (request, response) -> {
                        RequestContext context = RequestContext.from(request);
                        return services.profiles.getProfile(context, GetProfileRequest.byId(context.owner)).profile;
                    });
                    // Get someone elses profile
                    get("/:id", (request, response) -> {
                        String id = request.params("id");
                        UUID uuid = UUID.fromString(id);
                        return services.profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(uuid)).profile.toPublic();
                    });
                    get("/devices", (request, response) -> {
                        return services.devices.findDevices(RequestContext.from(request), services.jsonMapper.readValue(request.bodyAsBytes(), DeviceService.FindDevicesRequest.class));
                    });
                    post("/devices/trust", (request, response) -> {
                        RequestContext context = RequestContext.from(request);
                        context.assertNotAnonymous();
                        DeviceService.TrustDeviceRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), DeviceService.TrustDeviceRequest.class);
                        DeviceService.CreateDeviceResponse device = services.devices.createDevice(context, new DeviceService.CreateDeviceRequest(context.owner, req.deviceName));
                        PublicProfile profile = services.profiles.getProfile(context, GetProfileRequest.byId(context.owner)).profile.toPublic();
                        services.sessions.onDeviceRegistered(services.identityStore.getIdentity(), profile, req.token, device);
                        return new TrustDeviceResponse();
                    });
                    delete("/devices/:id", (request, response) -> {
                        RequestContext context = RequestContext.from(request);
                        String deviceId = request.params("id");
                        return services.devices.deleteDevice(context, new DeviceService.DeleteDeviceRequest(context.owner, Integer.parseInt(deviceId)));
                    });
                });

                path("/auth", () -> {
                    before("/*", (request, response) -> {
                        RequestContext.from(request).assertAnonymous();
                    });
                    // Login
                    get("/login", (request, response) -> {
                        LoginRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), LoginRequest.class);
                        return services.auth.login(RequestContext.from(request), req);
                    });
                    // Create an account
                    post("/create", (request, response) -> {
                        CreateAccountRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), CreateAccountRequest.class);
                        return services.auth.createAccount(RequestContext.from(request), req);
                    });
                    post("/verify", (request, response) -> {
                        VerifyAccountRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), VerifyAccountRequest.class);
                        return services.auth.verify(RequestContext.from(request), req);
                    });
                    post("/reset/request", (request, response) -> {
                        RequestPasswordResetRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), RequestPasswordResetRequest.class);
                        return services.auth.requestPasswordReset(RequestContext.from(request), req);
                    });
                    post("/reset/perform", (request, response) -> {
                        ResetPasswordRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), ResetPasswordRequest.class);
                        return services.auth.resetPassword(RequestContext.from(request), req);
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
                });
            });
        });

        // Reports server version
        // This contract is forever, please change with care!
        get("/api/version", (request, response) -> ServerVersion.version());
        // Query this route to discover what version of the APIs are supported and how the server is configured
        get("/api/discover", (request, response) -> {
            List<CollarVersion> versions = new ArrayList<>();
            versions.add(new CollarVersion(0, 1));
            List<CollarFeature> features = new ArrayList<>();
            features.add(new CollarFeature("auth:verification_scheme", configuration.minecraftSessionVerifier.getName()));
            features.add(new CollarFeature("groups:locations", true));
            features.add(new CollarFeature("groups:waypoints", true));
            return new DiscoverResponse(versions, features);
        });

        // Basic web interface. Not for production use.
        if (configuration.enableWeb) {
            LOGGER.log(Level.WARNING, "Builtin web interface is NOT for production use");
            get("/", (request, response) -> {
                response.redirect("/app/login");
                return "";
            });
            path("/app", () -> {
                before("/*", (request, response) -> {
                    response.header("Content-Type", "text/html; charset=UTF-8");
                });
                get("/login", (request, response) -> {
                    Cookie cookie = Cookie.from(services.tokenCrypter, request);
                    if (cookie == null) {
                        return render("login");
                    } else {
                        response.redirect("/app");
                        return "";
                    }
                }, Object::toString);
                post("/login", (request, response) -> {
                    String email = request.queryParamsSafe("email");
                    String password = request.queryParamsSafe("password");
                    Profile profile = services.auth.login(RequestContext.ANON, new LoginRequest(email, password)).profile;
                    if (!profile.emailVerified) {
                        throw new UnauthorisedException("email not verified");
                    }
                    setLoginCookie(services, response, profile);
                    response.redirect("/app");
                    return "";
                }, Object::toString);
                get("/logout", (request, response) -> {
                    Cookie.remove(response);
                    response.redirect("/app/login");
                    return "";
                }, Object::toString);
                get("/signup", (request, response) -> render("signup"), Object::toString);
                post("/signup", (request, response) -> {
                    String name = request.queryParamsSafe("name");
                    String email = request.queryParamsSafe("email");
                    String password = request.queryParamsSafe("password");
                    String confirmPassword = request.queryParamsSafe("confirmPassword");
                    PublicProfile profile = services.auth.createAccount(RequestContext.ANON, new CreateAccountRequest(email, name, password, confirmPassword)).profile;
                    Cookie cookie = new Cookie(profile.id, new Date().getTime() * TimeUnit.DAYS.toMillis(1));
                    cookie.set(services.tokenCrypter, response);
                    response.redirect("/app");
                    return "";
                }, Object::toString);
                get("", (request, response) -> {
                    Cookie cookie = Cookie.from(services.tokenCrypter, request);
                    if (cookie == null) {
                        response.redirect("/app/login");
                        return "";
                    } else {
                        Profile profile = services.profiles.getProfile(new RequestContext(cookie.profileId), GetProfileRequest.byId(cookie.profileId)).profile;
                        Map<String, Object> ctx = new HashMap<>();
                        ctx.put("name", profile.name);
                        return render(ctx, "home");
                    }
                }, Object::toString);

                get("/verify/", (request, response) -> {
                    String token = request.queryParams("token");
                    VerificationToken.from(configuration.tokenCrypter, token).ifPresent(aToken -> {
                        String redirect = services.auth.verify(RequestContext.ANON, new VerifyAccountRequest(token)).redirectUrl;
                        Profile profile = services.profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(aToken.profileId)).profile;
                        try {
                            setLoginCookie(services, response, profile);
                        } catch (IOException e) {
                            throw new ServerErrorException("server error", e);
                        }
                        response.redirect(redirect);
                    });
                    return "";
                }, Objects::toString);

                path("/devices", () -> {
                    get("/trust/:token", (request, response) -> {
                        Cookie cookie = Cookie.from(services.tokenCrypter, request);
                        if (cookie == null) {
                            response.redirect("/app/login");
                            return "";
                        } else {
                            Map<String, Object> ctx = new HashMap<>();
                            ctx.put("token", request.params("token"));
                            return render(ctx, "trust");
                        }
                    }, Object::toString);
                    post("/trust/:id", (request, response) -> {
                        Cookie cookie = Cookie.from(services.tokenCrypter, request);
                        if (cookie == null) {
                            response.redirect("/app/login");
                            return "";
                        } else {
                            String token = request.queryParams("token");
                            String name = request.queryParams("name");
                            RequestContext context = new RequestContext(cookie.profileId);
                            DeviceService.CreateDeviceResponse device = services.devices.createDevice(context, new DeviceService.CreateDeviceRequest(context.owner, name));
                            PublicProfile profile = services.profiles.getProfile(context, GetProfileRequest.byId(context.owner)).profile.toPublic();
                            services.sessions.onDeviceRegistered(services.identityStore.getIdentity(), profile, token, device);
                            response.redirect("/app");
                            return "";
                        }
                    }, Object::toString);
                });
            });
        }
        callback.accept(services);
        LOGGER.info("Collar server started.");
        LOGGER.info(services.urlProvider.homeUrl());
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

    private static String render(String templatePath) {
        return render(new HashMap<>(), templatePath);
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
            ApiToken token = ApiToken.deserialize(crypter, tokenString);
            if (new Date().after(new Date(token.expiresAt))) {
                throw new UnauthorisedException("old token");
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
