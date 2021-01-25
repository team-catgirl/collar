package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.security.KeyPairGeneratorException;
import team.catgirl.collar.server.common.ServerVersion;
import team.catgirl.collar.server.security.MemoryServerKeyPairProvider;
import team.catgirl.collar.server.security.ServerKeyPairProvider;
import team.catgirl.collar.utils.Utils;
import team.catgirl.collar.messages.ClientMessage;
import team.catgirl.collar.messages.ServerMessage.CreateGroupResponse;
import team.catgirl.collar.messages.ServerMessage.IdentificationSuccessful;
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

    public static void startServer() throws IOException, NoSuchAlgorithmException, KeyPairGeneratorException {
        String portValue = System.getenv("PORT");
        if (portValue != null) {
            port(Integer.parseInt(portValue));
        } else {
            port(3000);
        }

        // Load the server version
        ServerVersion version = ServerVersion.version();

        // Services
        ObjectMapper mapper = Utils.createObjectMapper();
        SessionManager sessions = new SessionManager(mapper);
        GroupManager groups = new GroupManager(sessions);
        ServerKeyPairProvider serverKeyPairProvider = new MemoryServerKeyPairProvider();

        // Always serialize objects returned as JSON
        defaultResponseTransformer(mapper::writeValueAsString);
        exception(HttpException.class, (e, request, response) -> {
            response.status(e.code);
            response.body(e.getMessage());
        });

        // Setup WebSockets
        webSocketIdleTimeoutMillis((int) TimeUnit.SECONDS.toMillis(60));
        webSocket("/api/1/listen", new WebSocketHandler(mapper, sessions, groups, serverKeyPairProvider));

        // Version routes
        path("/api/1", () -> {
            // Used to test if API is available
            get("/", (request, response) -> new ServerStatusResponse("OK"));
            // The servers current identity
            get("/identity", (request, response) -> serverKeyPairProvider.get().publicKey);
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
        private final ServerKeyPairProvider serverKeyPairProvider;

        public WebSocketHandler(ObjectMapper mapper, SessionManager manager, GroupManager groups, ServerKeyPairProvider serverKeyPairProvider) {
            this.mapper = mapper;
            this.manager = manager;
            this.groups = groups;
            this.serverKeyPairProvider = serverKeyPairProvider;
        }

        @OnWebSocketConnect
        public void connected(Session session) {
            LOGGER.log(Level.INFO, "New session started");
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
            if (message.identifyRequest != null) {
                if (message.identifyRequest.identity == null) {
                    manager.stopSession(session, "No valid identity", null);
                } else {
                    LOGGER.log(Level.INFO, "Identifying player " + message.identifyRequest.identity.player);
                    manager.identify(session, message.identifyRequest);
                    send(session, new IdentificationSuccessful(serverKeyPairProvider.get().publicKey).serverMessage());
                }
            }
            if (message.createGroupRequest != null) {
                CreateGroupResponse resp = groups.createGroup(message.createGroupRequest);
                send(session, resp.serverMessage());
                groups.sendMembershipRequests(message.createGroupRequest.me.player, resp.group, null);
            }
            if (message.acceptGroupMembershipRequest != null) {
                AcceptGroupMembershipResponse resp = groups.acceptMembership(message.acceptGroupMembershipRequest);
                send(session, resp.serverMessage());
            }
            if (message.leaveGroupRequest != null) {
                LeaveGroupResponse resp = groups.leaveGroup(message.leaveGroupRequest);
                send(session, resp.serverMessage());
                groups.updateGroup(message.leaveGroupRequest.groupId);
            }
            if (message.updatePlayerStateRequest != null) {
                UpdatePlayerStateResponse resp = groups.updatePosition(message.updatePlayerStateRequest);
                send(session, resp.serverMessage());
            }
            if (message.groupInviteRequest != null) {
                GroupInviteResponse resp = groups.invite(message.groupInviteRequest);
                send(session, resp.serverMessage());
            }
            if (message.ping != null) {
                LOGGER.log(Level.FINE, "Ping received");
                send(session, new Pong().serverMessage());
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
