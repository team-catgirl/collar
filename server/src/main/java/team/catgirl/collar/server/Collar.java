package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.messages.ClientMessage;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.messages.ServerMessage.CreateIdentityResponse;
import team.catgirl.collar.messages.ServerMessage.IdentificationResponse;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.server.http.SessionManager;
import team.catgirl.collar.server.security.ServerIdentityStore;
import team.catgirl.collar.server.services.groups.GroupService;

import java.io.IOException;
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
    private final GroupService groups;
    private final ServerIdentityStore identityStore;

    public Collar(ObjectMapper mapper, SessionManager sessions, GroupService groups, ServerIdentityStore identityStore) {
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
        PlayerIdentity player = sessions.getIdentity(session);
        if (player != null) {
            groups.removeUserFromAllGroups(player.player);
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
        if (message.pingRequest != null) {
            LOGGER.log(Level.FINE, "Ping received");
            send(session, new ServerMessage.PongResponse().serverMessage(identityStore.getIdentity()));
        } else if (sessions.isIdentified(session)) {
            if (message.createGroupRequest != null) {
                LOGGER.log(Level.INFO, "createGroupRequest");
                ServerMessage.CreateGroupResponse resp = groups.createGroup(message.identity, message.createGroupRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
                groups.sendMembershipRequests(message.identity, resp.group, null);
            } else if (message.acceptGroupMembershipRequest != null) {
                LOGGER.log(Level.INFO, "acceptGroupMembershipRequest");
                ServerMessage.AcceptGroupMembershipResponse resp = groups.acceptMembership(message.identity, message.acceptGroupMembershipRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            } else if (message.leaveGroupRequest != null) {
                LOGGER.log(Level.INFO, "leaveGroupRequest");
                ServerMessage.LeaveGroupResponse resp = groups.leaveGroup(message.identity, message.leaveGroupRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            } else if (message.updatePlayerStateRequest != null) {
                LOGGER.log(Level.INFO, "updatePlayerStateRequest");
                ServerMessage.UpdatePlayerStateResponse resp = groups.updatePosition(message.identity, message.updatePlayerStateRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            } else if (message.groupInviteRequest != null) {
                LOGGER.log(Level.INFO, "groupInviteRequest");
                ServerMessage.GroupInviteResponse resp = groups.invite(message.identity, message.groupInviteRequest);
                send(session, resp.serverMessage(identityStore.getIdentity()));
            } else {
                sessions.stopSession(session, "client sent an unknown message", null);
            }
        } else {
            if (message.createIdentityRequest != null) {
                LOGGER.log(Level.INFO, "createIdentityRequest");
                if (!identityStore.isTrustedIdentity(message.identity)) {
                    identityStore.trustIdentity(message.identity, message.createIdentityRequest);
                }
                send(session, new CreateIdentityResponse(identityStore.generatePreKeyBundle()).serverMessage(identityStore.getIdentity()));
            } else if (message.identifyRequest != null) {
                LOGGER.log(Level.INFO, "identifyRequest");
                IdentificationResponse.Status status;
                if (identityStore.isTrustedIdentity(message.identity)) {
                    status = IdentificationResponse.Status.SUCCESSS;
                    sessions.identify(session, message.identity);
                    send(session, new IdentificationResponse(status).serverMessage(identityStore.getIdentity()));
                } else {
                    status = IdentificationResponse.Status.FAILURE;
                }
                if (status == IdentificationResponse.Status.FAILURE) {
                    sessions.stopSession(session, "Identity is not trusted", null);
                }
            } else {
                sessions.stopSession(session, "client sent an unknown message", null);
            }
        }
    }

    private ClientMessage decodeMessage(Session session, String value) throws IOException {
        if (BaseEncoding.base64().canDecode(value) && sessions.isIdentified(session)) {
            PlayerIdentity playerIdentity = sessions.getIdentity(session);
            if (playerIdentity == null) {
                throw new IllegalStateException("encrypted message received before session was started");
            }
            byte[] decrypt = identityStore.createCypher().decrypt(playerIdentity, BaseEncoding.base64().decode(value));
            return mapper.readValue(decrypt, ClientMessage.class);
        } else {
            return mapper.readValue(value, ClientMessage.class);
        }
    }

    public void send(Session session, ServerMessage serverMessage) throws IOException {
        String message;
        if (canUseCrypto(session, serverMessage)) {
            byte[] bytes = identityStore.createCypher().crypt(identityStore.getIdentity(), mapper.writeValueAsBytes(serverMessage));
            message = BaseEncoding.base64().encode(bytes);
        } else {
            message = mapper.writeValueAsString(serverMessage);
        }
        try {
            session.getRemote().sendString(message);
        } catch (IOException e) {
            sessions.stopSession(session, "Could not send message to client", e);
            throw e;
        }
    }

    private boolean canUseCrypto(Session session, ServerMessage serverMessage) {
        return serverMessage.createIdentityResponse != null && serverMessage.identificationResponse != null && sessions.isIdentified(session);
    }
}
