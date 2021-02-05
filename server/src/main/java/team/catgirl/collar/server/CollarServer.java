package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.protocol.PacketIO;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;
import team.catgirl.collar.protocol.identity.IdentifyRequest;
import team.catgirl.collar.protocol.identity.IdentifyResponse;
import team.catgirl.collar.protocol.keepalive.KeepAliveRequest;
import team.catgirl.collar.protocol.keepalive.KeepAliveResponse;
import team.catgirl.collar.protocol.session.SessionFailedResponse.MojangVerificationFailedResponse;
import team.catgirl.collar.protocol.session.StartSessionRequest;
import team.catgirl.collar.protocol.session.StartSessionResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipRequest;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsTrustedRelationshipResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsUntrustedRelationshipResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.http.AppUrlProvider;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.protocol.ProtocolHandler;
import team.catgirl.collar.server.security.ServerIdentityStore;
import team.catgirl.collar.server.security.mojang.MinecraftSessionVerifier;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.services.profiles.ProfileService.GetProfileRequest;
import team.catgirl.collar.server.session.SessionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebSocket
public class CollarServer {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private final ObjectMapper mapper;
    private final SessionManager sessions;
    private final ServerIdentityStore identityStore;
    private final ProfileService profiles;
    private final AppUrlProvider urlProvider;
    private final MinecraftSessionVerifier minecraftSessionVerifier;
    private final List<ProtocolHandler> protocolHandlers;
    private final BiConsumer<ClientIdentity, MinecraftPlayer> sessionStopped;

    public CollarServer(ObjectMapper mapper, SessionManager sessions, ServerIdentityStore identityStore, ProfileService profiles, AppUrlProvider urlProvider, MinecraftSessionVerifier minecraftSessionVerifier, List<ProtocolHandler> protocolHandlers) {
        this.mapper = mapper;
        this.sessions = sessions;
        this.identityStore = identityStore;
        this.profiles = profiles;
        this.urlProvider = urlProvider;
        this.minecraftSessionVerifier = minecraftSessionVerifier;
        this.protocolHandlers = protocolHandlers;
        this.sessionStopped = (identity, player) -> protocolHandlers.forEach(protocolHandler -> protocolHandler.onSessionStopped(identity, player, protocolResponse -> {
            send(null, protocolResponse);
        }));
    }

    @OnWebSocketConnect
    public void connected(Session session) throws IOException {
        LOGGER.log(Level.INFO, "New socket connected");
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        LOGGER.log(Level.INFO, "Session closed " + statusCode + " " + reason);
        sessions.stopSession(session, reason, null, sessionStopped);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable e) {
        LOGGER.log(Level.SEVERE, "Unrecoverable error", e);
        sessions.stopSession(session, "Unrecoverable error", null, sessionStopped);
    }

    @OnWebSocketMessage
    public void message(Session session, InputStream is) throws IOException {
        ProtocolRequest req = read(session, is);
        LOGGER.log(Level.INFO, "Message from " + req.identity);
        ServerIdentity serverIdentity = identityStore.getIdentity();
        if (req instanceof KeepAliveRequest) {
            LOGGER.log(Level.INFO, "KeepAliveRequest received. Sending KeepAliveRequest.");
            sendPlain(session, new KeepAliveResponse(serverIdentity));
        } else if (req instanceof IdentifyRequest) {
            IdentifyRequest request = (IdentifyRequest)req;
            if (request.identity == null) {
                LOGGER.log(Level.INFO, "Signaling client to register");
                String token = sessions.createDeviceRegistrationToken(session);
                String url = urlProvider.deviceVerificationUrl(token);
                sendPlain(session, new RegisterDeviceResponse(serverIdentity, url));
            } else {
                PublicProfile profile;
                try {
                    profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(req.identity.id())).profile.toPublic();
                } catch (NotFoundException e) {
                    LOGGER.log(Level.SEVERE, "Profile " + request.identity.id() + " does not exist but the client thinks it should.");
                    sendPlain(session, new IsUntrustedRelationshipResponse(serverIdentity));
                    sessions.stopSession(session, "Identity " + request.identity.id() + " was not found", null, null);
                    return;
                }
                LOGGER.log(Level.INFO, "Profile found for " + req.identity.id());
                sendPlain(session, new IdentifyResponse(serverIdentity, profile));
            }
        } else if (req instanceof SendPreKeysRequest) {
            SendPreKeysRequest request = (SendPreKeysRequest) req;
            identityStore.trustIdentity(request);
            SendPreKeysResponse response = identityStore.createSendPreKeysResponse();
            sendPlain(session, response);
        } else if (req instanceof StartSessionRequest) {
            LOGGER.log(Level.INFO, "Starting session");
            StartSessionRequest request = (StartSessionRequest)req;
            if (minecraftSessionVerifier.verify(request.session)) {
                sendPlain(session, new StartSessionResponse(serverIdentity));
                sessions.identify(session, req.identity, request.session.toPlayer());
            } else {
                sendPlain(session, new MojangVerificationFailedResponse(serverIdentity, ((StartSessionRequest) req).session));
                sessions.stopSession(session, "Minecraft session invalid", null, sessionStopped);
            }
        } else if (req instanceof CheckTrustRelationshipRequest) {
            LOGGER.log(Level.INFO, "Checking if client/server have a trusted relationship");
            if (identityStore.isTrustedIdentity(req.identity)) {
                LOGGER.log(Level.INFO, "Identity is trusted. Signaling client to start encryption.");
                CheckTrustRelationshipResponse response = new IsTrustedRelationshipResponse(serverIdentity);
                sendPlain(session, response);
            } else {
                LOGGER.log(Level.INFO, "Identity is NOT trusted. Signaling client to restart registration.");
                CheckTrustRelationshipResponse response = new IsUntrustedRelationshipResponse(serverIdentity);
                sendPlain(session, response);
                sessions.stopSession(session, "Identity not trusted", null, null);
            }
        } else {
            protocolHandlers.stream()
                    .map(protocolHandler -> protocolHandler.handleRequest(this, req, response -> send(session, response)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("message received was not understood"));
        }
    }

    @Nonnull
    public ProtocolRequest read(@Nonnull Session session, @Nonnull InputStream message) {
        PacketIO packetIO = new PacketIO(mapper, identityStore.createCypher());
        try {
            return packetIO.decode(sessions.getIdentity(session), message, ProtocolRequest.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void send(@Nullable Session session, @Nonnull ProtocolResponse resp) {
        if (session != null && !session.isOpen()) {
            return;
        }
        if (resp instanceof BatchProtocolResponse) {
            BatchProtocolResponse batchResponse = (BatchProtocolResponse)resp;
            batchResponse.responses.forEach((response, identity) -> {
                Session anotherSession = sessions.getSession(identity);
                if (anotherSession != null) {
                    send(anotherSession, response);
                }
            });
        } else {
            PacketIO packetIO = new PacketIO(mapper, identityStore.createCypher());
            byte[] bytes;
            if (sessions.isIdentified(session)) {
                try {
                    bytes = packetIO.encodeEncrypted(sessions.getIdentity(session), resp);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                try {
                    bytes = packetIO.encodePlain(resp);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            sendBytes(session, bytes);
        }
    }

    public void sendPlain(@Nonnull Session session, @Nonnull ProtocolResponse resp) {
        if (!session.isOpen()) {
            return;
        }
        PacketIO packetIO = new PacketIO(mapper, null);
        byte[] bytes;
        try {
            bytes = packetIO.encodePlain(resp);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        sendBytes(session, bytes);
    }

    private void sendBytes(@Nonnull Session session, @Nonnull byte[] bytes) {
        session.getRemote().sendBytesByFuture(ByteBuffer.wrap(bytes));
    }
}
