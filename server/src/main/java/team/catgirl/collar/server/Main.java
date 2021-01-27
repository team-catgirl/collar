package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.common.ServerVersion;
import team.catgirl.collar.server.mongo.Mongo;
import team.catgirl.collar.server.security.ServerIdentityStore;
import team.catgirl.collar.server.security.signal.SignalServerIdentityStore;
import team.catgirl.collar.utils.Utils;
import team.catgirl.collar.messages.ClientMessage;
import team.catgirl.collar.messages.ServerMessage.CreateGroupResponse;
import team.catgirl.collar.messages.ServerMessage.IdentificationResponse;
import team.catgirl.collar.messages.ServerMessage.LeaveGroupResponse;
import team.catgirl.collar.messages.ServerMessage.UpdatePlayerStateResponse;
import team.catgirl.collar.server.http.HttpException;
import team.catgirl.collar.server.managers.GroupManager;
import team.catgirl.collar.server.managers.SessionManager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;
import static team.catgirl.collar.messages.ServerMessage.*;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        Utils.registerGPGProvider();
        startServer();
    }

    public static void startServer() throws IOException, NoSuchAlgorithmException {
        String portValue = System.getenv("PORT");
        if (portValue != null) {
            port(Integer.parseInt(portValue));
        } else {
            port(3000);
        }

        // Load the server version
        ServerVersion version = ServerVersion.version();

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
        webSocket("/api/1/listen", new WebSocketHandler(mapper, sessions, groups, serverIdentityStore));

        // Version routes
        path("/api/1", () -> {
            // Used to test if API is available
            get("/", (request, response) -> new ServerStatusResponse("OK"));
            // The servers current identity
            get("/identity", (request, response) -> identity);
        });

        // Server routes

        // Reports server version
        get("/api/version", (request, response) -> version);
        // Query this route to discover what version of the APIs are supported
        get("/api/version/discover", (request, response) -> {
            List<Integer> apiVersions = new ArrayList<>();
            apiVersions.add(1);
            return apiVersions;
        });
        // Return nothing
        get("/", (request, response) -> "", Object::toString);
    }


    @WebSocket
    public static class WebSocketHandler {
        private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

        private final ObjectMapper mapper;
        private final SessionManager manager;
        private final GroupManager groups;
        private final ServerIdentityStore identityStore;

        public WebSocketHandler(ObjectMapper mapper, SessionManager manager, GroupManager groups, ServerIdentityStore identityStore) {
            this.mapper = mapper;
            this.manager = manager;
            this.groups = groups;
            this.identityStore = identityStore;
        }

        @OnWebSocketConnect
        public void connected(Session session) {
            LOGGER.log(Level.INFO, "New session started");
            send(session, new ServerConnectedResponse().serverMessage(identityStore.getIdentity()));
        }

        @OnWebSocketClose
        public void closed(Session session, int statusCode, String reason) {
            LOGGER.log(Level.INFO, "Session closed " + statusCode + " " + reason);
            UUID player = manager.getPlayer(session);
            if (player != null) {
                groups.removeUserFromAllGroups(player);
            }
            manager.stopSession(session, "Session closed", null);
        }

        @OnWebSocketError
        public void onError(Throwable e) {
            LOGGER.log(Level.SEVERE, "There was an error", e);
        }

        @OnWebSocketMessage
        public void message(Session session, String value) throws IOException {
            ClientMessage message = mapper.readValue(value, ClientMessage.class);
            if (message.createIdentityRequest != null) {
                IdentificationResponse.Status status;
                if (identityStore.isTrustedIdentity(message.identity)) {
                    status = IdentificationResponse.Status.FAILURE;
                } else {
                    identityStore.createIdentity(message.identity, message.createIdentityRequest);
                    status = IdentificationResponse.Status.SUCCESSS;
                }
                send(session, new IdentificationResponse(status).serverMessage(identityStore.getIdentity()));
                if (status == IdentificationResponse.Status.FAILURE) {
                    manager.stopSession(session, "Identity is already trusted", null);
                }
            }
            if (message.identifyRequest != null) {
                IdentificationResponse.Status status;
                if (identityStore.isTrustedIdentity(message.identity)) {
                    status = IdentificationResponse.Status.SUCCESSS;
                    manager.identify(session, message.identity);
                    send(session, new IdentificationResponse(status).serverMessage(identityStore.getIdentity()));
                } else {
                    status = IdentificationResponse.Status.FAILURE;
                }
                if (status == IdentificationResponse.Status.FAILURE) {
                    manager.stopSession(session, "Identity is already trusted", null);
                }
            }
            if (message.createGroupRequest != null) {
                CreateGroupResponse resp = groups.createGroup(message.identity, message.createGroupRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
                groups.sendMembershipRequests(message.identity, resp.group, null);
            }
            if (message.acceptGroupMembershipRequest != null) {
                AcceptGroupMembershipResponse resp = groups.acceptMembership(message.identity, message.acceptGroupMembershipRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            }
            if (message.leaveGroupRequest != null) {
                LeaveGroupResponse resp = groups.leaveGroup(message.identity, message.leaveGroupRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
                groups.updateGroup(message.leaveGroupRequest.groupId);
            }
            if (message.updatePlayerStateRequest != null) {
                UpdatePlayerStateResponse resp = groups.updatePosition(message.identity, message.updatePlayerStateRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            }
            if (message.groupInviteRequest != null) {
                GroupInviteResponse resp = groups.invite(message.identity, message.groupInviteRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            }
            if (message.ping != null) {
                LOGGER.log(Level.FINE, "Ping received");
                send(session, new Pong().serverMessage(identityStore.getIdentity()));
            }
        }

        public void send(Session session, ServerMessage o) {
            try {
                String message = mapper.writeValueAsString(o);
                session.getRemote().sendString(message);
            } catch (IOException e) {
                manager.stopSession(session, "Could not send message to client", e);
            }
        }
    }

    private static class ServerStatusResponse {
        public final String status;

        public ServerStatusResponse(String status) {
            this.status = status;
        }
    }
}
