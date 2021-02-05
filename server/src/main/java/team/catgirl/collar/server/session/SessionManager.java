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
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Maps players UUIDs to sessions
 */
public final class SessionManager {

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private final ConcurrentMap<MinecraftPlayer, ClientIdentity> playerToClient = new ConcurrentHashMap<>();
    private final ConcurrentMap<Session, MinecraftPlayer> sessionToPlayer = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClientIdentity, Session> clientIdentityToSession = new ConcurrentHashMap<>();
    private final ConcurrentMap<Session, ClientIdentity> sessionToClientIdentity = new ConcurrentHashMap<>();

    // TODO: move all this to device service records
    private final ConcurrentMap<String, Session> devicesWaitingToRegister = new ConcurrentHashMap<>();

    private final ObjectMapper messagePack;
    private final ServerIdentityStore store;

    public SessionManager(ObjectMapper messagePack, ServerIdentityStore store) {
        this.messagePack = messagePack;
        this.store = store;
    }

    public void identify(Session session, ClientIdentity identity, MinecraftPlayer player) {
        playerToClient.put(player, identity);
        clientIdentityToSession.put(identity, session);
        sessionToClientIdentity.put(session, identity);
        sessionToPlayer.put(session, player);
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

    public boolean isIdentified(@Nullable Session session) {
        return clientIdentityToSession.containsValue(session);
    }

    public void stopSession(@Nonnull Session session,
                            @Nonnull String reason,
                            @Nullable IOException e,
                            @Nullable BiConsumer<ClientIdentity, MinecraftPlayer> callback) {
        // Run callback
        ClientIdentity clientIdentity = sessionToClientIdentity.get(session);
        MinecraftPlayer minecraftPlayer = sessionToPlayer.get(session);
        if (clientIdentity != null && callback != null) {
            callback.accept(clientIdentity, minecraftPlayer);
        }
        // Start removing state
        LOGGER.log(e == null ? Level.INFO : Level.SEVERE, reason, e);
        ClientIdentity playerIdentity = sessionToClientIdentity.remove(session);
        if (playerIdentity != null) {
            try {
                if (session.isOpen()) {
                    try {
                        send(session, playerIdentity, new SessionErrorResponse(store.getIdentity()));
                    } catch (IOException ioException) {
                        throw new IllegalStateException("Couldn't send SessionErrorResponse", e);
                    }
                }
            } finally {
                session = clientIdentityToSession.remove(playerIdentity);
                MinecraftPlayer player = sessionToPlayer.remove(session);
                playerToClient.remove(player);
            }
        } else {
            session.close(1000, "Session stopped");
        }
    }

    public void send(@Nonnull Session session, @Nullable ClientIdentity recipient, @Nonnull ProtocolResponse resp) throws IOException {
        PacketIO packetIO = new PacketIO(messagePack, store.createCypher());
        ByteBuffer buffer;
        if (isIdentified(session)) {
            buffer = ByteBuffer.wrap(packetIO.encodeEncrypted(recipient, resp));
        } else {
            buffer = ByteBuffer.wrap(packetIO.encodePlain(resp));
        }
        session.getRemote().sendBytes(buffer);
    }

    public Session getSession(MinecraftPlayer player) {
        ClientIdentity clientIdentity = playerToClient.get(player);
        return clientIdentity == null ? null : clientIdentityToSession.get(clientIdentity);
    }

    @Nullable
    public ClientIdentity getIdentity(@Nonnull Session session) {
        return sessionToClientIdentity.get(session);
    }

    @Nullable
    public ClientIdentity getIdentity(@Nonnull MinecraftPlayer player) {
        return playerToClient.get(player);
    }

    @Nonnull
    public List<MinecraftPlayer> findPlayers(@Nonnull ClientIdentity identity, @Nonnull List<UUID> players) {
        Session callerSession = clientIdentityToSession.get(identity);
        MinecraftPlayer callerPlayer = sessionToPlayer.get(callerSession);
        return sessionToPlayer.values().stream()
                .filter(callerPlayer::inServerWith)
                .filter(player -> players.contains(player.id))
                .collect(Collectors.toList());
    }

    @Nullable
    public MinecraftPlayer findPlayer(@Nonnull ClientIdentity identity) {
        Session session = clientIdentityToSession.get(identity);
        return sessionToPlayer.get(session);
    }

    @Nullable
    public Session getSession(@Nonnull ClientIdentity identity) {
        return clientIdentityToSession.get(identity);
    }
}
