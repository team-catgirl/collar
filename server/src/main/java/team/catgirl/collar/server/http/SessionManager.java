package team.catgirl.collar.server.http;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.protocol.DeviceRegisteredResponse;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.server.http.HttpException.NotFoundException;
import team.catgirl.collar.server.http.HttpException.ServerErrorException;
import team.catgirl.collar.server.services.devices.DeviceService.CreateDeviceResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
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

    // TODO: move all this to device service records
    private final Cache<String, Session> sessionsWaitingToRegister = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    private final Cache<String, KeyPair.PublicKey> registrationPublicKeys = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private final ObjectMapper mapper;

    public SessionManager(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void identify(Session session, PlayerIdentity identity) {
        sessionToIdentity.put(session, identity);
        identityToSession.put(identity, session);
        playerToIdentity.put(identity.player, identity);
    }

    public String createDeviceRegistrationToken(Session session, KeyPair.PublicKey publicKey) {
        String token = TokenGenerator.urlToken();
        sessionsWaitingToRegister.put(token, session);
        registrationPublicKeys.put(token, publicKey);
        return token;
    }

    public void onDeviceRegistered(ServerIdentity identity, String token, CreateDeviceResponse resp) {
        Session session = sessionsWaitingToRegister.getIfPresent(token);
        if (session == null) {
            throw new NotFoundException("session does not exist");
        }
        try {
            send(session, new DeviceRegisteredResponse(identity, resp.device.deviceId));
        } catch (IOException e) {
            throw new ServerErrorException("could not send DeviceRegisteredResponse", e);
        }
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

    public void send(Session session, ProtocolResponse resp) throws IOException {
        session.getRemote().sendString(mapper.writeValueAsString(resp));
    }

    public Session getSession(UUID player) {
        PlayerIdentity playerIdentity = playerToIdentity.get(player);
        return playerIdentity == null ? null : identityToSession.get(playerIdentity);
    }

    public PlayerIdentity getIdentity(Session session) {
        return sessionToIdentity.get(session);
    }

    public KeyPair.PublicKey getDeviceRegistrationPublicKey(String token) {
        return registrationPublicKeys.getIfPresent(token);
    }
}
