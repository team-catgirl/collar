package team.catgirl.collar.server.session;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.protocol.PacketIO;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.protocol.session.SessionFailedResponse.SessionErrorResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.security.ServerIdentityStore;
import team.catgirl.collar.server.services.devices.DeviceService.CreateDeviceResponse;

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

    // TODO: move all this to device service records
    private final ConcurrentMap<String, Session> devicesWaitingToRegister = new ConcurrentHashMap<>();

    private final ObjectMapper messagePack;
    private final ServerIdentityStore store;

    public SessionManager(ObjectMapper messagePack, ServerIdentityStore store) {
        this.messagePack = messagePack;
        this.store = store;
    }

    public void identify(Session session, ClientIdentity identity, MinecraftPlayer player) {
        SessionState state = new SessionState(session, identity, player);
        sessions.put(session, state);
    }

    public String createDeviceRegistrationToken(@Nonnull Session session) {
        String token = TokenGenerator.urlToken();
        devicesWaitingToRegister.put(token, session);
        return token;
    }

    public void onDeviceRegistered(@Nonnull ServerIdentity identity,
                                   @Nonnull PublicProfile profile,
                                   @Nonnull String token,
                                   @Nonnull CreateDeviceResponse resp) {
        Session session = devicesWaitingToRegister.get(token);
        if (session == null) {
            throw new NotFoundException("session does not exist");
        }
        try {
            send(session, null, new DeviceRegisteredResponse(identity, profile, resp.device.deviceId));
        } catch (IOException e) {
            throw new ServerErrorException("could not send DeviceRegisteredResponse", e);
        }
    }

    public boolean isIdentified(Session session) {
        return getIdentity(session).isPresent();
    }

    public void stopSession(Session session,
                            String reason,
                            IOException e,
                            BiConsumer<ClientIdentity, MinecraftPlayer> callback) {
        // Run callback
        SessionState state = sessions.get(session);
        if (state != null && callback != null) {
            callback.accept(state.identity, state.player);
        }
        // Start removing state
        LOGGER.log(e == null ? Level.INFO : Level.SEVERE, reason, e);
        SessionState sessionState = sessions.remove(session);
        if (sessionState != null) {
            if (session.isOpen()) {
                try {
                    send(session, sessionState.identity, new SessionErrorResponse(store.getIdentity()));
                } catch (IOException ioException) {
                    throw new IllegalStateException("Couldn't send SessionErrorResponse", e);
                }
            }
        } else {
            session.close(1000, "Session stopped");
        }
    }

    public void send(Session session, ClientIdentity recipient, ProtocolResponse resp) throws IOException {
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
        return sessions.values().stream().filter(sessionState -> sessionState.player.equals(player))
                .findFirst()
                .map(sessionState -> sessionState.identity);
    }

    public List<MinecraftPlayer> findPlayers(ClientIdentity identity, @Nonnull List<UUID> players) {
        if (identity == null) {
            return new ArrayList<>();
        }
        MinecraftPlayer player = findPlayer(identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + identity));
        return sessions.values().stream()
                .filter(sessionState -> sessionState.player.inServerWith(player))
                .filter(sessionState -> players.contains(sessionState.player.id))
                .map(sessionState -> sessionState.player)
                .collect(Collectors.toList());
    }

    public Optional<MinecraftPlayer> findPlayer(ClientIdentity identity) {
        if (identity == null) {
            return Optional.empty();
        }
        return sessions.values().stream()
                .filter(sessionState -> sessionState.identity.equals(identity))
                .findFirst()
                .map(sessionState -> sessionState.player);
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

    public Optional<MinecraftPlayer> findPlayer(ClientIdentity identity, UUID player) {
        return findPlayer(identity).flatMap(me -> {
            MinecraftPlayer candidate = new MinecraftPlayer(player, me.server);
            return sessions.values().stream().filter(sessionState -> sessionState.player.equals(candidate)).findFirst().map(sessionState -> sessionState.player);
        });
    }

    public static final class SessionState {
        public final Session session;
        public final ClientIdentity identity;
        public final MinecraftPlayer player;

        public SessionState(Session session, ClientIdentity identity, MinecraftPlayer player) {
            this.session = session;
            this.identity = identity;
            this.player = player;
        }
    }
}
