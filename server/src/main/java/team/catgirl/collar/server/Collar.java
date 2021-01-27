package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.messages.ClientMessage;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.server.managers.GroupManager;
import team.catgirl.collar.server.managers.SessionManager;
import team.catgirl.collar.server.security.ServerIdentityStore;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements the web socket protocol for collar
 */
@WebSocket
public class Collar {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private final ObjectMapper mapper;
    private final SessionManager sessions;
    private final GroupManager groups;
    private final ServerIdentityStore identityStore;

    public Collar(ObjectMapper mapper, SessionManager sessions, GroupManager groups, ServerIdentityStore identityStore) {
        this.mapper = mapper;
        this.sessions = sessions;
        this.groups = groups;
        this.identityStore = identityStore;
    }

    @OnWebSocketConnect
    public void connected(Session session) throws IOException {
        LOGGER.log(Level.INFO, "New session started");
        send(session, new ServerMessage.ServerConnectedResponse().serverMessage(identityStore.getIdentity()));
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        LOGGER.log(Level.INFO, "Session closed " + statusCode + " " + reason);
        UUID player = sessions.getPlayer(session);
        if (player != null) {
            groups.removeUserFromAllGroups(player);
        }
        sessions.stopSession(session, "Session closed", null);
    }

    @OnWebSocketError
    public void onError(Throwable e) {
        LOGGER.log(Level.SEVERE, "There was an error", e);
    }

    @OnWebSocketMessage
    public void message(Session session, String value) throws IOException {
        ClientMessage message = decodeMessage(session, value);
        if (message.ping != null) {
            LOGGER.log(Level.FINE, "Ping received");
            send(session, new ServerMessage.Pong().serverMessage(identityStore.getIdentity()));
        } else if (sessions.isIdentified(session)) {
            if (message.createGroupRequest != null) {
                ServerMessage.CreateGroupResponse resp = groups.createGroup(message.identity, message.createGroupRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
                groups.sendMembershipRequests(message.identity, resp.group, null);
            } else if (message.acceptGroupMembershipRequest != null) {
                ServerMessage.AcceptGroupMembershipResponse resp = groups.acceptMembership(message.identity, message.acceptGroupMembershipRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            } else if (message.leaveGroupRequest != null) {
                ServerMessage.LeaveGroupResponse resp = groups.leaveGroup(message.identity, message.leaveGroupRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            } else if (message.updatePlayerStateRequest != null) {
                ServerMessage.UpdatePlayerStateResponse resp = groups.updatePosition(message.identity, message.updatePlayerStateRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            } else if (message.groupInviteRequest != null) {
                ServerMessage.GroupInviteResponse resp = groups.invite(message.identity, message.groupInviteRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            } else {
                sessions.stopSession(session, "client sent an unknown message", null);
            }
        } else {
            if (message.createIdentityRequest != null) {
                ServerMessage.IdentificationResponse.Status status;
                if (identityStore.isTrustedIdentity(message.identity)) {
                    status = ServerMessage.IdentificationResponse.Status.FAILURE;
                } else {
                    identityStore.createIdentity(message.identity, message.createIdentityRequest);
                    status = ServerMessage.IdentificationResponse.Status.SUCCESSS;
                }
                send(session, new ServerMessage.IdentificationResponse(status).serverMessage(identityStore.getIdentity()));
                if (status == ServerMessage.IdentificationResponse.Status.FAILURE) {
                    sessions.stopSession(session, "Identity is already trusted", null);
                }
            } else if (message.identifyRequest != null) {
                ServerMessage.IdentificationResponse.Status status;
                if (identityStore.isTrustedIdentity(message.identity)) {
                    status = ServerMessage.IdentificationResponse.Status.SUCCESSS;
                    sessions.identify(session, message.identity);
                    send(session, new ServerMessage.IdentificationResponse(status).serverMessage(identityStore.getIdentity()));
                } else {
                    status = ServerMessage.IdentificationResponse.Status.FAILURE;
                }
                if (status == ServerMessage.IdentificationResponse.Status.FAILURE) {
                    sessions.stopSession(session, "Identity is not trusted", null);
                }
            } else {
                sessions.stopSession(session, "client sent an unknown message", null);
            }
        }
    }

    private ClientMessage decodeMessage(Session session, String value) throws IOException {
        if (sessions.isIdentified(session) || BaseEncoding.base64().canDecode(value)) {
            byte[] decrypt = identityStore.createCypher().decrypt(identityStore.getIdentity(), BaseEncoding.base64().decode(value));
            return mapper.readValue(decrypt, ClientMessage.class);
        } else {
            return mapper.readValue(value, ClientMessage.class);
        }
    }

    public void send(Session session, ServerMessage serverMessage) throws IOException {
        String message;
        if (sessions.isIdentified(session)) {
            // Session is available, crypt a server messages
            byte[] bytes = identityStore.createCypher().crypt(identityStore.getIdentity(), mapper.writeValueAsBytes(serverMessage));
            message = BaseEncoding.base64().encode(bytes);
        } else {
            // No session is available - use plain text!
            message = mapper.writeValueAsString(serverMessage);
        }
        try {
            session.getRemote().sendString(message);
        } catch (IOException e) {
            sessions.stopSession(session, "Could not send message to client", e);
            throw e;
        }
    }
}
