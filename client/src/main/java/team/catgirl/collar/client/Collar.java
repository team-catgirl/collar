package team.catgirl.collar.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import io.mikael.urlbuilder.UrlBuilder;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.whispersystems.libsignal.IdentityKey;
import team.catgirl.collar.api.http.CollarVersion;
import team.catgirl.collar.api.http.DiscoverResponse;
import team.catgirl.collar.client.CollarException.ConnectionException;
import team.catgirl.collar.client.CollarException.UnsupportedServerVersionException;
import team.catgirl.collar.client.api.features.AbstractFeature;
import team.catgirl.collar.client.api.features.ApiListener;
import team.catgirl.collar.client.api.groups.GroupsFeature;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.client.security.IdentityState;
import team.catgirl.collar.client.security.signal.ResettableClientIdentityStore;
import team.catgirl.collar.client.security.signal.SignalClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;
import team.catgirl.collar.protocol.identity.IdentifyRequest;
import team.catgirl.collar.protocol.identity.IdentifyResponse;
import team.catgirl.collar.protocol.keepalive.KeepAliveResponse;
import team.catgirl.collar.protocol.session.StartSessionRequest;
import team.catgirl.collar.protocol.session.StartSessionResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipRequest;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsTrustedRelationshipResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsUntrustedRelationshipResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.KeyPair.PublicKey;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftSession;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Collar {
    private static final Logger LOGGER = Logger.getLogger(Collar.class.getName());

    private static final CollarVersion VERSION = new CollarVersion(0, 1);

    private final MinecraftSession minecraftSession;
    private final UrlBuilder baseUrl;
    private final HomeDirectory home;
    private final CollarListener listener;
    private final OkHttpClient http;
    private WebSocket webSocket;
    private State state;
    private final Map<Class<?>, AbstractFeature<? extends ApiListener>> features;
    private Consumer<ProtocolRequest> sender;
    private ResettableClientIdentityStore identityStore;
    private Supplier<ClientIdentityStore> identityStoreSupplier;

    private Collar(OkHttpClient http, MinecraftSession minecraftSession, UrlBuilder baseUrl, HomeDirectory home, CollarListener listener) {
        this.http = http;
        this.minecraftSession = minecraftSession;
        this.baseUrl = baseUrl;
        this.home = home;
        this.listener = listener;
        changeState(State.DISCONNECTED);
        this.features = new HashMap<>();
        this.identityStoreSupplier = () -> identityStore;
        this.features.put(GroupsFeature.class, new GroupsFeature(this, identityStoreSupplier, request -> sender.accept(request)));
    }

    /**
     * Create a new Collar client
     * @param session of the minecraft player
     * @param server address of the collar server
     * @param minecraftHome the minecraft data directory
     * @param listener client listener
     * @return collar client
     * @throws IOException setting up
     */
    public static Collar create(MinecraftSession session, String server, File minecraftHome, CollarListener listener) throws IOException {
        OkHttpClient http = new OkHttpClient();
        UrlBuilder baseUrl = UrlBuilder.fromString(server);
        return new Collar(http, session, baseUrl, HomeDirectory.from(minecraftHome, baseUrl.hostName), listener);
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

    /**
     * @return groups api
     */
    public GroupsFeature groups() {
        return (GroupsFeature)features.get(GroupsFeature.class);
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
        DiscoverResponse response = httpGet(http, baseUrl.withPath("/api/discover").toString(), DiscoverResponse.class);
        StringJoiner versions = new StringJoiner(",");
        response.versions.stream()
                .peek(collarVersion -> versions.add(versions.toString()))
                .filter(collarVersion -> collarVersion.equals(VERSION))
                .findFirst()
                .orElseThrow(() -> new UnsupportedServerVersionException(VERSION + " is not supported by server. Server supports versions " + versions.toString()));
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
        private final Collar collar;
        private KeepAlive keepAlive;
        private ServerIdentity serverIdentity;

        public CollarWebSocket(Collar collar) {
            this.collar = collar;
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            // Create the sender delegate
            sender = request -> {
                sendRequest(webSocket, request);
            };
            LOGGER.log(Level.INFO, "Connection established");
            if (SignalClientIdentityStore.hasIdentityStore(home)) {
                identityStore = getOrCreateIdentityKeyStore(webSocket, null);
            } else {
                sendRequest(webSocket, IdentifyRequest.unknown());
            }
            // Start the keep alive
            this.keepAlive = new KeepAlive(webSocket);
            this.keepAlive.start(null);
        }

        private ResettableClientIdentityStore getOrCreateIdentityKeyStore(WebSocket webSocket, UUID owner) {
            return new ResettableClientIdentityStore(() -> SignalClientIdentityStore.from(owner, home, signalProtocolStore -> {
                LOGGER.log(Level.INFO, "New installation. Registering device with server...");
                IdentityKey publicKey = signalProtocolStore.getIdentityKeyPair().getPublicKey();
                new IdentityState(owner).write(home);
                ClientIdentity clientIdentity = new ClientIdentity(owner, new PublicKey(publicKey.getFingerprint(), publicKey.serialize()));
                IdentifyRequest request = new IdentifyRequest(clientIdentity);
                sendRequest(webSocket, request);
            }, (store) -> {
                LOGGER.log(Level.INFO, "Existing installation. Loading the store and identifying with server");
                IdentityKey publicKey = store.getIdentityKeyPair().getPublicKey();
                IdentityState identityState = IdentityState.read(home);
                ClientIdentity clientIdentity = new ClientIdentity(identityState.owner, new PublicKey(publicKey.getFingerprint(), publicKey.serialize()));
                IdentifyRequest request = new IdentifyRequest(clientIdentity);
                sendRequest(webSocket, request);
            }));
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
            ClientIdentity identity = identityStore == null ? null : identityStore.currentIdentity();
            if (resp instanceof IdentifyResponse) {
                IdentifyResponse response = (IdentifyResponse) resp;
                if (identityStore == null) {
                    identityStore = getOrCreateIdentityKeyStore(webSocket, response.profile.id);
                    identity = identityStore.currentIdentity();
                    StartSessionRequest request = new StartSessionRequest(identity, minecraftSession);
                    sendRequest(webSocket, request);
                    keepAlive.stop();
                    keepAlive.start(identity);
                }
            } else if (resp instanceof KeepAliveResponse) {
                LOGGER.log(Level.INFO, "KeepAliveResponse received");
            } else if (resp instanceof RegisterDeviceResponse) {
                RegisterDeviceResponse registerDeviceResponse = (RegisterDeviceResponse)resp;
                LOGGER.log(Level.INFO, "RegisterDeviceResponse received with registration url " + ((RegisterDeviceResponse) resp).approvalUrl);
                listener.onConfirmDeviceRegistration(collar, registerDeviceResponse);
            } else if (resp instanceof DeviceRegisteredResponse) {
                DeviceRegisteredResponse response = (DeviceRegisteredResponse)resp;
                identityStore = getOrCreateIdentityKeyStore(webSocket, response.profile.id);
                identityStore.setDeviceId(response.deviceId);
                LOGGER.log(Level.INFO, "Ready to exchange keys for device " + response.deviceId);
                SendPreKeysRequest request = identityStore.createSendPreKeysRequest();
                sendRequest(webSocket, request);
            } else if (resp instanceof SendPreKeysResponse) {
                SendPreKeysResponse response = (SendPreKeysResponse)resp;
                if (identityStore == null) {
                    throw new IllegalStateException("identity has not been established");
                }
                identityStore.trustIdentity(response);
                LOGGER.log(Level.INFO, "PreKeys have been exchanged successfully");
                sendRequest(webSocket, new StartSessionRequest(identity, minecraftSession));
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
            } else {
                // Find the first Feature that handles the request and return the result
                boolean wasHandled = collar.features.values().stream()
                        .map(feature -> feature.handleResponse(resp))
                        .filter(handled -> handled)
                        .findFirst()
                        .orElse(false);
                if (!wasHandled) {
                    throw new IllegalStateException("Did not understand received protocol response " + message);
                }
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

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
