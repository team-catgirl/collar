package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.RegisterDeviceRequest;
import team.catgirl.collar.protocol.RegisterDeviceResponse;
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
    }

    @OnWebSocketError
    public void onError(Throwable e) {
        LOGGER.log(Level.SEVERE, "Error", e);
    }

    @OnWebSocketMessage
    public void message(Session session, String value) throws IOException {
        LOGGER.log(Level.INFO, "Received message " + value);
        ProtocolRequest req = mapper.readValue(value, ProtocolRequest.class);
        if (req instanceof RegisterDeviceRequest) {
            String token = sessions.createDeviceRegistrationToken(session, req.identity.publicKey);
            String deviceApprovalUrl = urlProvider.deviceVerificationUrl(token);
            RegisterDeviceResponse response = new RegisterDeviceResponse(identityStore.getIdentity(), deviceApprovalUrl);
            send(session, response);
        }
    }

    public void send(Session session, ProtocolResponse resp) throws IOException {
        String message = mapper.writeValueAsString(resp);
        try {
            session.getRemote().sendString(message);
        } catch (IOException e) {
            sessions.stopSession(session, "Could not send message to client", e);
            throw e;
        }
    }
}
