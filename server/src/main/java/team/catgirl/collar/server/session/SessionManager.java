package team.catgirl.collar.server.session;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.services.devices.DeviceService.CreateDeviceResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
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
    private final Cache<String, Session> devicesWaitingToRegister = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private final ObjectMapper mapper;

    public SessionManager(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void identify(Session session, ClientIdentity identity, MinecraftPlayer player) {
        playerToClient.put(player, identity);
        clientIdentityToSession.put(identity, session);
        sessionToClientIdentity.put(session, identity);
        sessionToPlayer.put(session, player);
    }

    public String createDeviceRegistrationToken(Session session) {
        String token = TokenGenerator.urlToken();
        devicesWaitingToRegister.put(token, session);
        return token;
    }

    public void onDeviceRegistered(ServerIdentity identity, PublicProfile profile, String token, CreateDeviceResponse resp) {
        Session session = devicesWaitingToRegister.getIfPresent(token);
        if (session == null) {
            throw new NotFoundException("session does not exist");
        }
        try {
            send(session, new DeviceRegisteredResponse(identity, profile, resp.device.deviceId));
        } catch (IOException e) {
            throw new ServerErrorException("could not send DeviceRegisteredResponse", e);
        }
    }

    public boolean isIdentified(Session session) {
        return clientIdentityToSession.containsValue(session);
    }

    public void stopSession(Session session, String reason, IOException e) {
        ClientIdentity playerIdentity = sessionToClientIdentity.remove(session);
        if (playerIdentity != null) {
            session = clientIdentityToSession.remove(playerIdentity);
            MinecraftPlayer player = sessionToPlayer.remove(session);
            playerToClient.remove(player);
        }
        session.close(1000, reason);
        LOGGER.log(e == null ? Level.INFO : Level.SEVERE, reason, e);
    }

    public void send(Session session, ProtocolResponse resp) throws IOException {
        session.getRemote().sendString(mapper.writeValueAsString(resp));
    }

    public Session getSession(MinecraftPlayer player) {
        ClientIdentity clientIdentity = playerToClient.get(player);
        return clientIdentity == null ? null : clientIdentityToSession.get(clientIdentity);
    }

    public ClientIdentity getIdentity(Session session) {
        return sessionToClientIdentity.get(session);
    }

    public ClientIdentity getIdentity(MinecraftPlayer player) {
        return playerToClient.get(player);
    }

    public List<MinecraftPlayer> findPlayers(ClientIdentity identity, List<UUID> players) {
        Session callerSession = clientIdentityToSession.get(identity);
        MinecraftPlayer callerPlayer = sessionToPlayer.get(callerSession);
        return sessionToPlayer.values().stream()
                .filter(callerPlayer::inServerWith)
                .filter(player -> players.contains(player.id))
                .collect(Collectors.toList());
    }

    public MinecraftPlayer findPlayer(ClientIdentity identity) {
        Session session = clientIdentityToSession.get(identity);
        return sessionToPlayer.get(session);
    }

    public Session getSession(ClientIdentity identity) {
        return clientIdentityToSession.get(identity);
    }
}
