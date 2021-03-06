package team.catgirl.collar.server.session;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.PacketIO;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.session.SessionFailedResponse.SessionErrorResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.security.cipher.CipherException;
import team.catgirl.collar.server.security.ServerIdentityStore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class SessionManager {

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private final ConcurrentMap<Session, SessionState> sessions = new ConcurrentHashMap<>();

    private final ObjectMapper messagePack;
    private final ServerIdentityStore store;

    public SessionManager(ObjectMapper messagePack, ServerIdentityStore store) {
        this.messagePack = messagePack;
        this.store = store;
    }

    public void identify(Session session, ClientIdentity identity, MinecraftPlayer player) {
        SessionState state = new SessionState(session, identity, player);
        sessions.compute(session, (theSession, sessionState) -> {
            if (sessionState != null) {
                throw new IllegalStateException("session cannot be identified more than once");
            }
            return state;
        });
    }

    public boolean isIdentified(Session session) {
        return getIdentity(session).isPresent();
    }


    public void stopSession(Session session,
                            String reason,
                            Throwable e,
                            BiConsumer<ClientIdentity, Player> callback) {
        // Run callback
        SessionState state = sessions.get(session);
        if (state != null && callback != null) {
            callback.accept(state.identity, state.toPlayer());
        }
        // Start removing state
        LOGGER.log(e == null ? Level.INFO : Level.SEVERE, reason, e);
        SessionState sessionState = sessions.remove(session);
        if (sessionState != null) {
            if (session.isOpen()) {
                try {
                    send(session, sessionState.identity, new SessionErrorResponse(store.getIdentity(), reason));
                } catch (IOException | CipherException ex) {
                    throw new IllegalStateException("Couldn't send SessionErrorResponse", ex);
                }
            }
        } else {
            session.close(1000, "Session stopped");
        }
    }

    public void send(Session session, ClientIdentity recipient, ProtocolResponse resp) throws IOException, CipherException {
        PacketIO packetIO = new PacketIO(messagePack, store.createCypher());
        ByteBuffer buffer;
        if (isIdentified(session)) {
            buffer = ByteBuffer.wrap(packetIO.encodeEncrypted(recipient, resp));
        } else {
            buffer = ByteBuffer.wrap(packetIO.encodePlain(resp));
        }
        session.getRemote().sendBytes(buffer);
    }

    public Optional<ClientIdentity> getIdentity(Session session) {
        if (session == null) {
            return Optional.empty();
        }
        SessionState sessionState = sessions.get(session);
        return sessionState == null ? Optional.empty() : Optional.of(sessionState.identity);
    }

    public Optional<ClientIdentity> getIdentity(MinecraftPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        return sessions.values().stream().filter(sessionState -> sessionState.minecraftPlayer.equals(player))
                .findFirst()
                .map(sessionState -> sessionState.identity);
    }

    public List<Player> findPlayers(ClientIdentity identity, @Nonnull List<UUID> players) {
        if (identity == null) {
            return new ArrayList<>();
        }
        MinecraftPlayer player = findMinecraftPlayer(identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + identity));
        return sessions.values().stream()
                .filter(sessionState -> sessionState.minecraftPlayer.inServerWith(player))
                .filter(sessionState -> players.contains(sessionState.minecraftPlayer.id))
                .map(SessionState::toPlayer)
                .collect(Collectors.toList());
    }

    public Optional<MinecraftPlayer> findMinecraftPlayer(ClientIdentity identity) {
        if (identity == null) {
            return Optional.empty();
        }
        return sessions.values().stream()
                .filter(sessionState -> sessionState.identity.equals(identity))
                .findFirst()
                .map(sessionState -> sessionState.minecraftPlayer);
    }

    public Optional<Player> findPlayer(ClientIdentity identity) {
        if (identity == null) {
            return Optional.empty();
        }
        return sessions.values().stream()
                .filter(sessionState -> sessionState.identity.equals(identity))
                .findFirst()
                .map(SessionState::toPlayer);
    }

    public Optional<Session> getSession(ClientIdentity identity) {
        if (identity == null) {
            return Optional.empty();
        }
        return sessions.values().stream()
                .filter(sessionState -> sessionState.identity.equals(identity))
                .findFirst()
                .map(sessionState -> sessionState.session);
    }

    public Optional<SessionState> getSessionStateByOwner(UUID owner) {
        return sessions.values().stream()
                .filter(sessionState -> sessionState.identity.owner.equals(owner))
                .findFirst();
    }

    public Optional<SessionState> getSessionStateByPlayer(UUID player) {
        return sessions.values().stream()
                .filter(sessionState -> sessionState.minecraftPlayer.id.equals(player))
                .findFirst();
    }

    public Optional<ClientIdentity> getIdentity(Player player) {
        return sessions.values().stream().filter(sessionState -> sessionState.identity.owner.equals(player.profile))
                .findAny()
                .map(sessionState -> sessionState.identity);
    }

    public Optional<ClientIdentity> getIdentityByMinecraftPlayerId(UUID playerId) {
        return sessions.values().stream().filter(sessionState -> sessionState.minecraftPlayer.id.equals(playerId))
                .findAny()
                .map(sessionState -> sessionState.identity);
    }

    public Optional<Player> findPlayerByProfile(UUID profile) {
        return sessions.values().stream().filter(sessionState -> sessionState.identity.owner.equals(profile))
                .findFirst()
                .map(SessionState::toPlayer);
    }

    public static final class SessionState {
        public final Session session;
        public final ClientIdentity identity;
        public final MinecraftPlayer minecraftPlayer;

        public SessionState(Session session, ClientIdentity identity, MinecraftPlayer minecraftPlayer) {
            this.session = session;
            this.identity = identity;
            this.minecraftPlayer = minecraftPlayer;
        }

        public Player toPlayer() {
            return new Player(identity.id(), minecraftPlayer);
        }
    }
}
