package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.server.mongo.Mongo;
import team.catgirl.collar.server.profiles.ProfileService;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;
import static team.catgirl.collar.messages.ServerMessage.*;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        startServer();
    }

    public static void startServer() throws NoSuchAlgorithmException {
        String portValue = System.getenv("PORT");
        if (portValue != null) {
            port(Integer.parseInt(portValue));
        } else {
            port(3000);
        }

        // Services
        MongoDatabase database = Mongo.database();
        ObjectMapper mapper = Utils.createObjectMapper();
        SessionManager sessions = new SessionManager(mapper);
        GroupManager groups = new GroupManager(sessions);
        ProfileService profiles = new ProfileService(database);

        // Always serialize objects returned as JSON
        defaultResponseTransformer(mapper::writeValueAsString);
        exception(HttpException.class, (e, request, response) -> {
            response.status(e.code);
            response.body(e.getMessage());
        });

        // Setup WebSockets
        webSocketIdleTimeoutMillis((int) TimeUnit.SECONDS.toMillis(60));
        webSocket("/api/1/coordshare/listen", new WebSocketHandler(mapper, sessions, groups));

        // Start routes
        get("/api/1/status/", (request, response) -> "OK");
        get("/", (request, response) -> {
            return "OwO";
        });
    }


    @WebSocket
    public static class WebSocketHandler {
        private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

        private final ObjectMapper mapper;
        private final SessionManager manager;
        private final GroupManager groups;

        public WebSocketHandler(ObjectMapper mapper, SessionManager manager, GroupManager groups) {
            this.mapper = mapper;
            this.manager = manager;
            this.groups = groups;
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
                if (message.identifyRequest.identity == null || message.identifyRequest.identity.id == null || message.identifyRequest.identity.player == null) {
                    manager.stopSession(session, "No valid identity", null);
                } else {
                    LOGGER.log(Level.INFO, "Identifying player " + message.identifyRequest.identity.player);
                    manager.identify(session, message.identifyRequest.identity);
                    send(session, new IdentificationSuccessful().serverMessage());
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
}
