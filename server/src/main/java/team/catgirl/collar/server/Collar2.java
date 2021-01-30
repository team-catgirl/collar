package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.devices.RegisterDeviceRequest;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;
import team.catgirl.collar.protocol.keepalive.KeepAliveRequest;
import team.catgirl.collar.protocol.keepalive.KeepAliveResponse;
import team.catgirl.collar.protocol.session.StartSessionRequest;
import team.catgirl.collar.protocol.session.StartSessionResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipRequest;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsTrustedRelationshipResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsUntrustedRelationshipResponse;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.http.AppUrlProvider;
import team.catgirl.collar.server.http.SessionManager;
import team.catgirl.collar.server.security.ServerIdentityStore;
import team.catgirl.collar.server.services.devices.DeviceService;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebSocket
public class Collar2 {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private final ObjectMapper mapper;
    private final SessionManager sessions;
    private final ServerIdentityStore identityStore;
    private final DeviceService devices;
    private final AppUrlProvider urlProvider;

    public Collar2(ObjectMapper mapper, SessionManager sessions, ServerIdentityStore identityStore, DeviceService devices, AppUrlProvider urlProvider) {
        this.mapper = mapper;
        this.sessions = sessions;
        this.identityStore = identityStore;
        this.devices = devices;
        this.urlProvider = urlProvider;
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
        } else if (req instanceof RegisterDeviceRequest) {
            LOGGER.log(Level.INFO, "Received RegisterDeviceRequest");
            String token = sessions.createDeviceRegistrationToken(session, req.identity.publicKey);
            String deviceApprovalUrl = urlProvider.deviceVerificationUrl(token);
            RegisterDeviceResponse response = new RegisterDeviceResponse(serverIdentity, deviceApprovalUrl);
            LOGGER.log(Level.INFO, "Sending RegisterDeviceResponse");
            sendPlain(session, response);
        } else if (req instanceof SendPreKeysRequest) {
            SendPreKeysRequest request = (SendPreKeysRequest) req;
            identityStore.trustIdentity(request);
            SendPreKeysResponse response = identityStore.createSendPreKeysResponse();
            sendPlain(session, response);
        } else if (req instanceof StartSessionRequest) {
            LOGGER.log(Level.INFO, "Starting session");
            sendPlain(session, new StartSessionResponse(serverIdentity));
            sessions.identify(session, req.identity);
        } else if (req instanceof CheckTrustRelationshipRequest) {
            LOGGER.log(Level.INFO, "Checking if client/server have a trusted relationship");
            if (identityStore.isTrustedIdentity(req.identity)) {
                LOGGER.log(Level.INFO, "Identity is trusted. Signaling client to start encryption.");
                sendPlain(session, new IsTrustedRelationshipResponse(serverIdentity));
            } else {
                LOGGER.log(Level.INFO, "Identity is NOT trusted. Signaling client to restart registration.");
                sendPlain(session, new IsUntrustedRelationshipResponse(serverIdentity));
            }
        } else {
            throw new IllegalStateException("message received was not understood");
        }
    }

    public void send(Session session, ProtocolResponse resp) throws IOException {
        String message;
        if (sessions.isIdentified(session)) {
            PlayerIdentity playerIdentity = sessions.getIdentity(session);
            byte[] bytes = identityStore.createCypher().crypt(playerIdentity, mapper.writeValueAsBytes(resp));
            message = BaseEncoding.base64().encode(bytes);
        } else {
            message = mapper.writeValueAsString(resp);
        }
        try {
            session.getRemote().sendString(message);
        } catch (IOException e) {
            sessions.stopSession(session, "Could not send message to client", e);
            throw e;
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
