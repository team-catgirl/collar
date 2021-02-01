package team.catgirl.collar.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;
import team.catgirl.collar.protocol.identity.IdentifyRequest;
import team.catgirl.collar.protocol.identity.IdentifyResponse;
import team.catgirl.collar.protocol.keepalive.KeepAliveRequest;
import team.catgirl.collar.protocol.keepalive.KeepAliveResponse;
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
import team.catgirl.collar.server.http.AppUrlProvider;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.protocol.ProtocolHandler;
import team.catgirl.collar.server.security.ServerIdentityStore;
import team.catgirl.collar.server.security.mojang.MinecraftSessionVerifier;
import team.catgirl.collar.server.services.devices.DeviceService;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.session.SessionManager;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebSocket
public class CollarServer {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private final ObjectMapper mapper;
    private final SessionManager sessions;
    private final ServerIdentityStore identityStore;
    private final DeviceService devices;
    private final ProfileService profiles;
    private final AppUrlProvider urlProvider;
    private final MinecraftSessionVerifier minecraftSessionVerifier;
    private final List<ProtocolHandler> protocolHandlers;

    public CollarServer(ObjectMapper mapper, SessionManager sessions, ServerIdentityStore identityStore, DeviceService devices, ProfileService profiles, AppUrlProvider urlProvider, MinecraftSessionVerifier minecraftSessionVerifier, List<ProtocolHandler> protocolHandlers) {
        this.mapper = mapper;
        this.sessions = sessions;
        this.identityStore = identityStore;
        this.devices = devices;
        this.profiles = profiles;
        this.urlProvider = urlProvider;
        this.minecraftSessionVerifier = minecraftSessionVerifier;
        this.protocolHandlers = protocolHandlers;
    }

    @OnWebSocketConnect
    public void connected(Session session) throws IOException {
        LOGGER.log(Level.INFO, "New socket connected");
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        LOGGER.log(Level.INFO, "Session closed " + statusCode + " " + reason);
        sessions.stopSession(session, reason, null);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable e) {
        LOGGER.log(Level.SEVERE, "Unrecoverable error", e);
        sessions.stopSession(session, "Unrecoverable error", null);
    }

    @OnWebSocketMessage
    public void message(Session session, String value) throws IOException {
        LOGGER.log(Level.INFO, "Received message " + value);
        ProtocolRequest req = mapper.readValue(value, ProtocolRequest.class);
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
                    profile = profiles.getProfile(RequestContext.SERVER, ProfileService.GetProfileRequest.byId(req.identity.id())).profile.toPublic();
                } catch (NotFoundException e) {
                    throw new IllegalStateException("Profile " + request.identity.id() + " does not exist but the client thinks it should");
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
                sessions.stopSession(session, "Minecraft session invalid", null);
            }
        } else if (req instanceof CheckTrustRelationshipRequest) {
            LOGGER.log(Level.INFO, "Checking if client/server have a trusted relationship");
            CheckTrustRelationshipResponse response;
            if (identityStore.isTrustedIdentity(req.identity)) {
                LOGGER.log(Level.INFO, "Identity is trusted. Signaling client to start encryption.");
                response = new IsTrustedRelationshipResponse(serverIdentity);
            } else {
                LOGGER.log(Level.INFO, "Identity is NOT trusted. Signaling client to restart registration.");
                response = new IsUntrustedRelationshipResponse(serverIdentity);
            }
            sendPlain(session, response);
        } else {
            protocolHandlers.stream()
                    .map(protocolHandler -> protocolHandler.handleRequest(this, req, response -> send(session, response)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("message received was not understood"));
        }
    }

    public void send(Session session, ProtocolResponse resp) {
        if (resp instanceof BatchProtocolResponse) {
            BatchProtocolResponse batchResponse = (BatchProtocolResponse)resp;
            batchResponse.responses.forEach((identity, response) -> {
                Session anotherSession = sessions.getSession(identity);
                send(anotherSession, response);
            });
        } else {
            String message;
            if (sessions.isIdentified(session)) {
                ClientIdentity clientIdentity = sessions.getIdentity(session);
                byte[] bytes = new byte[0];
                try {
                    bytes = identityStore.createCypher().crypt(clientIdentity, mapper.writeValueAsBytes(resp));
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Could not write message", e);
                }
                message = BaseEncoding.base64().encode(bytes);
            } else {
                try {
                    message = mapper.writeValueAsString(resp);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Could not write message", e);
                }
            }
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                sessions.stopSession(session, "Could not send message to client", e);
                throw new IllegalStateException("Could not send message", e);
            }
        }
    }

    public void sendPlain(Session session, ProtocolResponse resp) throws IOException {
        try {
            session.getRemote().sendString(mapper.writeValueAsString(resp));
        } catch (IOException e) {
            sessions.stopSession(session, "Could not send plain message to client", e);
            throw e;
        }
    }
}
