package team.catgirl.collar.server.managers;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.security.PlayerIdentity;

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

    private final ConcurrentMap<Session, PlayerIdentity> sessionToIdentity = new ConcurrentHashMap<>();
    private final ConcurrentMap<PlayerIdentity, Session> identityToSession = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, PlayerIdentity> playerToIdentity = new ConcurrentHashMap<>();

    private final ObjectMapper mapper;

    public SessionManager(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void identify(Session session, PlayerIdentity identity) {
        // TODO: register the identity if we haven't seen it before
        // TODO: otherwise, check that the client is really them by verifying the signature
        sessionToIdentity.put(session, identity);
        identityToSession.put(identity, session);
        playerToIdentity.put(identity.player, identity);
    }

    public boolean isIdentified(Session session) {
        return sessionToIdentity.containsKey(session);
    }

    public void stopSession(Session session, String reason, IOException e) {
        PlayerIdentity playerIdentity = sessionToIdentity.remove(session);
        if (playerIdentity != null) {
            playerToIdentity.remove(playerIdentity.player);
            identityToSession.remove(playerIdentity);
        }
        session.close(1000, reason);
        LOGGER.log(e == null ? Level.INFO : Level.SEVERE, reason, e);
    }

    public void send(Session session, ServerMessage o) throws IOException {
        session.getRemote().sendString(mapper.writeValueAsString(o));
    }

    public Session getSession(UUID player) {
        PlayerIdentity playerIdentity = playerToIdentity.get(player);
        return playerIdentity == null ? null : identityToSession.get(playerIdentity);
    }

    public UUID getPlayer(Session session) {
        PlayerIdentity playerIdentity = sessionToIdentity.get(session);
        return playerIdentity == null ? null : playerIdentity.player;
    }
}
