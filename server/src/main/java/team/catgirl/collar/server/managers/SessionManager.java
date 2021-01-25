package team.catgirl.collar.server.managers;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.messages.ClientMessage;
import team.catgirl.collar.messages.ClientMessage.IdentifyRequest;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.models.Identity;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maps players UUIDs to sessions
 */
public final class SessionManager {

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private final ConcurrentMap<Session, Identity> sessionToIdentity = new ConcurrentHashMap<>();
    private final ConcurrentMap<Identity, Session> identityToSession = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Identity> playerToIdentity = new ConcurrentHashMap<>();

    private final ObjectMapper mapper;

    public SessionManager(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void identify(Session session, IdentifyRequest identifyRequest) {
        // TODO: register the identity if we haven't seen it before
        // TODO: otherwise, check that the client is really them by verifying the signature
        sessionToIdentity.put(session, identifyRequest.identity);
        identityToSession.put(identifyRequest.identity, session);
        playerToIdentity.put(identifyRequest.identity.player, identifyRequest.identity);
    }

    public void stopSession(Session session, String reason, IOException e) {
        Identity identity = sessionToIdentity.remove(session);
        if (identity != null) {
            playerToIdentity.remove(identity.player);
            identityToSession.remove(identity);
        }
        session.close(1000, reason);
        LOGGER.log(e == null ? Level.INFO : Level.SEVERE, reason, e);
    }

    public void send(Session session, ServerMessage o) throws IOException {
        session.getRemote().sendString(mapper.writeValueAsString(o));
    }

    public Session getSession(UUID player) {
        Identity playerIdentity = playerToIdentity.get(player);
        return playerIdentity == null ? null : identityToSession.get(playerIdentity);
    }

    public UUID getPlayer(Session session) {
        Identity identity = sessionToIdentity.get(session);
        return identity == null ? null : identity.player;
    }
}
