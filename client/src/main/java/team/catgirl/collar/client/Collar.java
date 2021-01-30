package team.catgirl.collar.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import io.mikael.urlbuilder.UrlBuilder;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.whispersystems.libsignal.IdentityKey;
import team.catgirl.collar.client.CollarClientException.ConnectionException;
import team.catgirl.collar.client.CollarClientException.UnsupportedVersionException;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.client.security.signal.SignalClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.protocol.devices.RegisterDeviceRequest;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;
import team.catgirl.collar.protocol.keepalive.KeepAliveResponse;
import team.catgirl.collar.protocol.session.StartSessionRequest;
import team.catgirl.collar.protocol.session.StartSessionResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipRequest;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsTrustedRelationshipResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsUntrustedRelationshipResponse;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.KeyPair.PublicKey;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Collar {
    private static final Logger LOGGER = Logger.getLogger(Collar.class.getName());

    private static final int VERSION = 1;

    private final UUID playerId;
    private final UrlBuilder baseUrl;
    private final HomeDirectory home;
    private final CollarListener listener;
    private final OkHttpClient http;
    private WebSocket webSocket;
    private State state;

    private Collar(OkHttpClient http, UUID playerId, UrlBuilder baseUrl, HomeDirectory home, CollarListener listener) {
        this.http = http;
        this.playerId = playerId;
        this.baseUrl = baseUrl;
        this.home = home;
        this.listener = listener;
        changeState(State.DISCONNECTED);
    }

    /**
     * Create a new Collar client
     * @param playerId of the minecraft player
     * @param server address of the collar server
     * @param minecraftHome the minecraft data directory
     * @param listener client listener
     * @return collar client
     * @throws IOException setting up
     */
    public static Collar create(UUID playerId, String server, File minecraftHome, CollarListener listener) throws IOException {
        OkHttpClient http = new OkHttpClient();
        UrlBuilder baseUrl = UrlBuilder.fromString(server);
        return new Collar(http, playerId, baseUrl, HomeDirectory.from(minecraftHome, playerId), listener);
    }

    /**
     * Connect to server
     */
    public void connect() {
        checkVersionCompatibility(http, baseUrl);
        String url = baseUrl.withPath("/api/1/listen").toString();
        LOGGER.log(Level.INFO, "Connecting to server " + url);
        webSocket = http.newWebSocket(new Request.Builder().url(url).build(), new CollarWebSocket(this));
        webSocket.request();
        http.dispatcher().executorService().shutdown();
        changeState(State.CONNECTING);
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        if (this.webSocket != null && state != State.DISCONNECTED) {
            LOGGER.log(Level.INFO, "Disconnected");
            this.webSocket.close(1000, "Client was disconnected");
            this.webSocket = null;
            changeState(State.DISCONNECTED);
        }
    }

    public State getState() {
        return state;
    }

    /**
     * Change the client state and fire the listener
     * @param state to change to
     */
    private void changeState(State state) {
        State previousState = this.state;
        if (previousState != state) {
            this.state = state;
            if (state == State.DISCONNECTED) {
                disconnect();
            }
            if (previousState == null) {
                LOGGER.log(Level.INFO, "Client in state " + state);
            } else {
                LOGGER.log(Level.INFO, "State changed from " + previousState + " to " + state);
            }
            this.listener.onStateChanged(this, state);
        }
    }

    /**
     * Test that the client version is supported by the server
     * @param http client
     */
    private static void checkVersionCompatibility(OkHttpClient http, UrlBuilder baseUrl) {
        int[] supportedVersions = httpGet(http, baseUrl.withPath("/api/discover").toString(), int[].class);
        StringJoiner versions = new StringJoiner(",");
        Arrays.stream(supportedVersions).forEach(value -> versions.add(String.valueOf(value)));
        if (Arrays.stream(supportedVersions).noneMatch(value -> value == VERSION)) {
            throw new UnsupportedVersionException("version " + VERSION + " is not supported by server. Server supports versions " + versions.toString());
        }
        LOGGER.log(Level.INFO, "Server supports versions " + versions);
    }

    private static <T> T httpGet(OkHttpClient http, String url, Class<T> aClass) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.code() == 200) {
                byte[] bytes = Objects.requireNonNull(response.body()).bytes();
                return Utils.createObjectMapper().readValue(bytes, aClass);
            } else {
                throw new ConnectionException("Failed to connect to server");
            }
        } catch (IOException ignored) {
            throw new ConnectionException("Failed to connect to server");
        }
    }

    class CollarWebSocket extends WebSocketListener {
        private final ObjectMapper mapper = Utils.createObjectMapper();
        private ClientIdentityStore identityStore;
        private final Collar collar;
        private KeepAlive keepAlive;
        private ServerIdentity serverIdentity;

        public CollarWebSocket(Collar collar) {
            this.collar = collar;
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            LOGGER.log(Level.INFO, "Connection established");
            try {
                this.identityStore = SignalClientIdentityStore.from(playerId, home, signalProtocolStore -> {
                    LOGGER.log(Level.INFO, "New installation. Registering device with server...");
                    IdentityKey publicKey = signalProtocolStore.getIdentityKeyPair().getPublicKey();
                    PlayerIdentity playerIdentity = new PlayerIdentity(playerId, new PublicKey(publicKey.getFingerprint(), publicKey.serialize()));
                    RegisterDeviceRequest request = new RegisterDeviceRequest(playerIdentity);
                    sendRequest(webSocket, request);
                }, store -> {
                    IdentityKey publicKey = store.getIdentityKeyPair().getPublicKey();
                    PlayerIdentity playerIdentity = new PlayerIdentity(playerId, new PublicKey(publicKey.getFingerprint(), publicKey.serialize()));
                    StartSessionRequest request = new StartSessionRequest(playerIdentity);
                    sendRequest(webSocket, request);
                });
            } catch (IOException e) {
                throw new IllegalStateException("could not setup identity store", e);
            }

            // Start the keep alive
            this.keepAlive = new KeepAlive(webSocket, this.identityStore.currentIdentity());
            this.keepAlive.start();
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            LOGGER.log(Level.SEVERE, "Closed socket: " + reason);
            this.keepAlive.stop();
            collar.changeState(State.DISCONNECTED);
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            LOGGER.log(Level.SEVERE, "Socket failure", t);
            collar.changeState(State.DISCONNECTED);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String message) {
            LOGGER.log(Level.INFO, "Message received " + message);
            ProtocolResponse resp = readResponse(message);
            PlayerIdentity identity = identityStore.currentIdentity();
            if (resp instanceof RegisterDeviceResponse) {
                RegisterDeviceResponse registerDeviceResponse = (RegisterDeviceResponse)resp;
                LOGGER.log(Level.INFO, "RegisterDeviceResponse received with registration url " + ((RegisterDeviceResponse) resp).approvalUrl);
                listener.onConfirmDeviceRegistration(collar, registerDeviceResponse);
            } else if (resp instanceof DeviceRegisteredResponse) {
                DeviceRegisteredResponse response = (DeviceRegisteredResponse)resp;
                identityStore.setDeviceId(response.deviceId);
                LOGGER.log(Level.INFO, "Ready to exchange keys for device " + response.deviceId);
                SendPreKeysRequest request = identityStore.createSendPreKeysRequest();
                sendRequest(webSocket, request);
            } else if (resp instanceof SendPreKeysResponse) {
                SendPreKeysResponse response = (SendPreKeysResponse)resp;
                identityStore.trustIdentity(response);
                LOGGER.log(Level.INFO, "PreKeys have been exchanged successfully");
                sendRequest(webSocket, new StartSessionRequest(identity));
            } else if (resp instanceof StartSessionResponse) {
                LOGGER.log(Level.INFO, "Session has started. Checking if the client and server are in a trusted relationship");
                sendRequest(webSocket, new CheckTrustRelationshipRequest(identity));
            } else if (resp instanceof IsTrustedRelationshipResponse) {
                LOGGER.log(Level.INFO, "Server has confirmed a trusted relationship with the client");
                this.serverIdentity = resp.identity;
                collar.changeState(State.CONNECTED);
            } else if (resp instanceof IsUntrustedRelationshipResponse) {
                LOGGER.log(Level.INFO, "Server has declared the client as untrusted. Consumer should reset the identity store and reconnect.");
                collar.changeState(State.DISCONNECTED);
                listener.onClientUntrusted(collar, identityStore);
            } else if (resp instanceof KeepAliveResponse) {
                LOGGER.log(Level.INFO, "KeepAliveResponse received");
            } else {
                throw new IllegalStateException("Did not understand received protocol response " + message);
            }
        }

        private ProtocolResponse readResponse(String message) {
            ProtocolResponse resp;
            if (BaseEncoding.base64().canDecode(message)) {
                byte[] bytes = identityStore.createCypher().decrypt(serverIdentity, BaseEncoding.base64().decode(message));
                try {
                    resp = mapper.readValue(bytes, ProtocolResponse.class);
                } catch (IOException e) {
                    throw new ConnectionException("Could not read message", e);
                }
            } else {
                try {
                    resp = mapper.readValue(message, ProtocolResponse.class);
                } catch (IOException e) {
                    throw new ConnectionException("Could not read message", e);
                }
            }
            return resp;
        }

        void sendRequest(WebSocket webSocket, ProtocolRequest req) {
            if (state == State.CONNECTED) {
                Cypher cypher = identityStore.createCypher();
                byte[] bytes;
                try {
                    bytes = mapper.writeValueAsBytes(req);
                } catch (JsonProcessingException e) {
                    throw new ConnectionException("Could not send message", e);
                }
                String message = BaseEncoding.base64().encode(cypher.crypt(serverIdentity, bytes));
                webSocket.send(message);
            } else {
                String message;
                try {
                    message = mapper.writeValueAsString(req);
                } catch (JsonProcessingException e) {
                    throw new ConnectionException("Could not send message", e);
                }
                webSocket.send(message);
            }
        }
    }
}
