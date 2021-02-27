package team.catgirl.collar.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.whispersystems.libsignal.IdentityKey;
import team.catgirl.collar.api.http.CollarFeature;
import team.catgirl.collar.api.http.CollarVersion;
import team.catgirl.collar.api.http.DiscoverResponse;
import team.catgirl.collar.client.CollarException.ConnectionException;
import team.catgirl.collar.client.CollarException.UnsupportedServerVersionException;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.api.ApiListener;
import team.catgirl.collar.client.api.friends.FriendsApi;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.api.identity.IdentityApi;
import team.catgirl.collar.client.api.location.LocationApi;
import team.catgirl.collar.client.api.messaging.MessagingApi;
import team.catgirl.collar.client.api.textures.TexturesApi;
import team.catgirl.collar.client.sdht.cipher.GroupContentCipher;
import team.catgirl.collar.client.sdht.cipher.ContentCiphers;
import team.catgirl.collar.client.minecraft.Ticks;
import team.catgirl.collar.client.sdht.SDHTApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.client.security.PrivateIdentity;
import team.catgirl.collar.client.security.ProfileState;
import team.catgirl.collar.client.security.signal.ResettableClientIdentityStore;
import team.catgirl.collar.client.security.signal.SignalClientIdentityStore;
import team.catgirl.collar.protocol.PacketIO;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;
import team.catgirl.collar.protocol.identity.IdentifyRequest;
import team.catgirl.collar.protocol.identity.IdentifyResponse;
import team.catgirl.collar.protocol.keepalive.KeepAliveResponse;
import team.catgirl.collar.protocol.session.SessionFailedResponse;
import team.catgirl.collar.protocol.session.SessionFailedResponse.MojangVerificationFailedResponse;
import team.catgirl.collar.protocol.session.StartSessionRequest;
import team.catgirl.collar.protocol.session.StartSessionResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipRequest;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsTrustedRelationshipResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsUntrustedRelationshipResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.PublicKey;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.security.mojang.MinecraftSession;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Collar {
    private static final Logger LOGGER = Logger.getLogger(Collar.class.getName());
    private static final CollarVersion VERSION = new CollarVersion(0, 1);

    public final CollarConfiguration configuration;
    private final GroupsApi groupsApi;
    private final LocationApi locationApi;
    private final TexturesApi texturesApi;
    private final FriendsApi friendsApi;
    private final IdentityApi identityApi;
    private final MessagingApi messagingApi;
    private final SDHTApi sdhtApi;
    private WebSocket webSocket;
    private volatile State state;
    private final List<AbstractApi<? extends ApiListener>> apis;
    private Consumer<ProtocolRequest> sender;
    private ResettableClientIdentityStore identityStore;
    private final Supplier<ClientIdentityStore> identityStoreSupplier;
    private final Ticks ticks;
    private final ContentCiphers recordCiphers;

    private Collar(CollarConfiguration configuration) {
        this.configuration = configuration;
        changeState(State.DISCONNECTED);
        this.identityStoreSupplier = () -> identityStore;
        Consumer<ProtocolRequest> sender = request -> this.sender.accept(request);
        this.ticks = configuration.ticks;
        this.recordCiphers = new ContentCiphers();
        this.apis = new ArrayList<>();
        this.sdhtApi = new SDHTApi(this, identityStoreSupplier, sender, recordCiphers, this.ticks);
        this.groupsApi = new GroupsApi(this, identityStoreSupplier, sender, sdhtApi);
        this.recordCiphers.register(new GroupContentCipher(groupsApi, identityStoreSupplier));
        this.locationApi = new LocationApi(this,
                identityStoreSupplier,
                sender,
                ticks,
                groupsApi,
                sdhtApi,
                configuration.playerLocation,
                configuration.entitiesSupplier);
        this.texturesApi = new TexturesApi(this, identityStoreSupplier, sender);
        this.identityApi = new IdentityApi(this, identityStoreSupplier, sender);
        this.messagingApi = new MessagingApi(this, identityStoreSupplier, sender);
        this.friendsApi = new FriendsApi(this, identityStoreSupplier, sender);
        this.apis.add(groupsApi);
        this.apis.add(locationApi);
        this.apis.add(texturesApi);
        this.apis.add(friendsApi);
        this.apis.add(identityApi);
        this.apis.add(messagingApi);
        this.apis.add(sdhtApi);
    }

    /**
     * Create a new Collar client
     * @param configuration of the client
     */
    public static Collar create(CollarConfiguration configuration) {
        return new Collar(configuration);
    }

    /**
     * Connect to server
     */
    public void connect() {
        checkServerCompatibility(configuration);
        String url = UrlBuilder.fromUrl(configuration.collarServerURL).withPath("/api/1/listen").toString();
        LOGGER.log(Level.INFO, "Connecting to server " + url);
        webSocket = Utils.http().newWebSocket(new Request.Builder().url(url).build(), new CollarWebSocket(this));
        webSocket.request();
        changeState(State.CONNECTING);
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        if (this.webSocket != null) {
            LOGGER.log(Level.INFO, "Disconnected");
            this.webSocket.cancel();
            this.webSocket = null;
            if (this.identityStore != null) {
                this.identityStore.clearAllGroupSessions();
            }
            if (state == State.DISCONNECTED) {
                return;
            }
            changeState(State.DISCONNECTED);
        }
    }

    /**
     * @return groups api
     */
    public GroupsApi groups() {
        assertConnected();
        return groupsApi;
    }

    /**
     * @return location api
     */
    public LocationApi location() {
        assertConnected();
        return locationApi;
    }

    /**
     * @return texture api
     */
    public TexturesApi textures() {
        assertConnected();
        return texturesApi;
    }

    /**
     * @return identity api
     */
    public IdentityApi identities() {
        assertConnected();
        return identityApi;
    }

    /**
     * @return friends api
     */
    public FriendsApi friends() {
        assertConnected();
        return friendsApi;
    }

    /**
     * @return messaging api
     */
    public MessagingApi messaging() {
        assertConnected();
        return messagingApi;
    }

    /**
     * @return client state
     */
    public State getState() {
        return state;
    }

    /**
     * @return the current clients identity
     */
    public ClientIdentity identity() {
        return identityStore != null ? identityStore.currentIdentity() : null;
    }

    /**
     * Change the client state and fire the listener
     * @param state to change to
     */
    private void changeState(State state) {
        State previousState = this.state;
        if (previousState != state) {
            this.state = state;
            if (previousState != null && previousState != State.DISCONNECTED && state == State.DISCONNECTED) {
                disconnect();
            }
            if (previousState == null) {
                LOGGER.log(Level.INFO, "client in state " + state);
            } else {
                if (identityStore != null) {
                    LOGGER.log(Level.INFO, identityStore.currentIdentity() + " state changed from " + previousState + " to " + state);
                } else {
                    LOGGER.log(Level.INFO, "state changed from " + previousState + " to " + state);
                }
            }
            if (previousState != null) {
                this.configuration.listener.onStateChanged(this, state);
                apis.forEach(abstractApi -> abstractApi.onStateChanged(state));
            }
        } else {
            throw new IllegalStateException("Cannot change state " + state + " to the same state");
        }
    }

    /**
     * Test that the client version is supported by the server and that the client is configured correctly for its features
     * @param configuration of the client
     */
    private static void checkServerCompatibility(CollarConfiguration configuration) {
        DiscoverResponse response = httpGet(UrlBuilder.fromUrl(configuration.collarServerURL).withPath("/api/discover").toString(), DiscoverResponse.class);
        StringJoiner versions = new StringJoiner(",");
        response.versions.stream()
                .peek(collarVersion -> versions.add(collarVersion.major + "." + collarVersion.minor))
                .filter(collarVersion -> collarVersion.equals(VERSION))
                .findFirst()
                .orElseThrow(() -> new UnsupportedServerVersionException(VERSION + " is not supported by server. Server supports versions " + versions.toString()));
        Optional<CollarFeature> verificationScheme = findFeature(response, "auth:verification_scheme");
        if (!verificationScheme.isPresent()) {
            throw new IllegalStateException("Does not have feature auth:verification_scheme");
        }
        verificationScheme.ifPresent(collarFeature -> {
            MinecraftSession minecraftSession = configuration.sessionSupplier.get();
            LOGGER.log(Level.INFO, "Server supports versions " + versions);
            if ("mojang".equals(collarFeature.value) && minecraftSession.clientToken == null && minecraftSession.accessToken == null) {
                throw new IllegalStateException("mojang verification scheme requested but was provided an invalid MinecraftSession");
            } else if ("nojang".equals(collarFeature.value) && minecraftSession.clientToken != null && minecraftSession.accessToken != null) {
                throw new IllegalStateException("nojang verification scheme requested but was provided an invalid MinecraftSession");
            }
        });
        findFeature(response, "groups:locations").orElseThrow(() -> new IllegalStateException("Server does not support groups:locations"));
        findFeature(response, "groups:waypoints").orElseThrow(() -> new IllegalStateException("Server does not support groups:waypoints"));
        findFeature(response, "profile:friends").orElseThrow(() -> new IllegalStateException("Server does not support profile:friends"));
    }

    private static Optional<CollarFeature> findFeature(DiscoverResponse response, String feature) {
        return response.features.stream()
                .filter(collarFeature -> feature.equals(collarFeature.name))
                .findFirst();
    }

    private static <T> T httpGet(String url, Class<T> aClass) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = Utils.http().newCall(request).execute()) {
            if (response.code() == 200) {
                byte[] bytes = Objects.requireNonNull(response.body()).bytes();
                return Utils.jsonMapper().readValue(bytes, aClass);
            } else {
                throw new ConnectionException("Failed to connect to server");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Connection issue", e);
            throw new ConnectionException("Failed to connect to server");
        }
    }

    /**
     * The current player
     * @return player
     */
    public MinecraftPlayer player() {
        return configuration.sessionSupplier.get().toPlayer();
    }

    private void assertConnected() {
        if (state != State.CONNECTED) {
            throw new IllegalStateException("Cannot use the API until client is CONNECTED");
        }
    }

    class CollarWebSocket extends WebSocketListener {
        private final ObjectMapper mapper = Utils.messagePackMapper();
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
                if (state != State.CONNECTED) {
                    throw new IllegalStateException("Client is not in CONNECTED state");
                }
                sendRequest(webSocket, request);
            };
            LOGGER.log(Level.INFO, "Connection established");
            if (SignalClientIdentityStore.hasIdentityStore(configuration.homeDirectory)) {
                identityStore = getOrCreateIdentityKeyStore(webSocket, null);
            } else {
                sendRequest(webSocket, IdentifyRequest.unknown());
            }
            // Start the keep alive
            this.keepAlive = new KeepAlive(this, webSocket);
            this.keepAlive.start(null);
        }

        private ResettableClientIdentityStore getOrCreateIdentityKeyStore(WebSocket webSocket, UUID owner) {
            if (ProfileState.exists(configuration.homeDirectory)) {
                owner = ProfileState.read(configuration.homeDirectory).owner;
            }
            UUID finalOwner = owner;
            return new ResettableClientIdentityStore(() -> SignalClientIdentityStore.from(finalOwner, configuration.homeDirectory, signalProtocolStore -> {
                LOGGER.log(Level.INFO, "New installation. Registering device with server");
                new ProfileState(finalOwner).write(configuration.homeDirectory);
            }, (store) -> {
                LOGGER.log(Level.INFO, "Existing installation. Loading the store and identifying with server " + serverIdentity);
                IdentityKey publicKey = store.getIdentityKeyPair().getPublicKey();
                ClientIdentity clientIdentity = new ClientIdentity(finalOwner, new PublicKey(publicKey.serialize()), null);
                IdentifyRequest request = new IdentifyRequest(clientIdentity);
                sendRequest(webSocket, request);
            }));
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            LOGGER.log(Level.SEVERE, "Closed socket: " + reason);
            this.keepAlive.stop();
            if (state != State.DISCONNECTED) {
                collar.changeState(State.DISCONNECTED);
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            LOGGER.log(Level.SEVERE, "Socket failure", t);
            if (state != State.DISCONNECTED) {
                collar.changeState(State.DISCONNECTED);
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString message) {
            ProtocolResponse resp = readResponse(message.toByteArray());
            LOGGER.log(Level.INFO, resp.getClass().getSimpleName() + " from " + resp.identity);
            ClientIdentity identity = identityStore == null ? null : identityStore.currentIdentity();
            if (resp instanceof IdentifyResponse) {
                IdentifyResponse response = (IdentifyResponse) resp;
                if (identityStore == null) {
                    identityStore = getOrCreateIdentityKeyStore(webSocket, response.profile.id);
                }
                StartSessionRequest request = new StartSessionRequest(identity, configuration.sessionSupplier.get());
                sendRequest(webSocket, request);
                keepAlive.stop();
                keepAlive.start(identity);
            } else if (resp instanceof KeepAliveResponse) {
                LOGGER.log(Level.INFO, "KeepAliveResponse received");
            } else if (resp instanceof RegisterDeviceResponse) {
                RegisterDeviceResponse registerDeviceResponse = (RegisterDeviceResponse)resp;
                LOGGER.log(Level.INFO, "RegisterDeviceResponse received with registration url " + ((RegisterDeviceResponse) resp).approvalUrl);
                configuration.listener.onConfirmDeviceRegistration(collar, registerDeviceResponse.approvalToken, registerDeviceResponse.approvalUrl);
            } else if (resp instanceof DeviceRegisteredResponse) {
                DeviceRegisteredResponse response = (DeviceRegisteredResponse)resp;
                identityStore = getOrCreateIdentityKeyStore(webSocket, response.profile.id);
                identityStore.processDeviceRegisteredResponse(response);
                LOGGER.log(Level.INFO, "Ready to exchange keys for device " + response.deviceId);
                SendPreKeysRequest request = identityStore.createSendPreKeysRequest(response);
                sendRequest(webSocket, request);
            } else if (resp instanceof SendPreKeysResponse) {
                SendPreKeysResponse response = (SendPreKeysResponse)resp;
                if (identityStore == null) {
                    throw new IllegalStateException("identity has not been established");
                }
                identityStore.trustIdentity(response.identity, response.preKeyBundle);
                LOGGER.log(Level.INFO, "PreKeys have been exchanged successfully");
                sendRequest(webSocket, new IdentifyRequest(identity));
            } else if (resp instanceof StartSessionResponse) {
                LOGGER.log(Level.INFO, "Session has started. Checking if the client and server are in a trusted relationship");
                sendRequest(webSocket, new CheckTrustRelationshipRequest(identity));
            } else if (resp instanceof SessionFailedResponse) {
                LOGGER.log(Level.INFO, "SessionFailedResponse received");
                if (resp instanceof MojangVerificationFailedResponse) {
                    MojangVerificationFailedResponse response = (MojangVerificationFailedResponse)resp;
                    LOGGER.log(Level.INFO, "SessionFailedResponse with mojang session verification failure");
                    configuration.listener.onMinecraftAccountVerificationFailed(collar, response.minecraftSession);
                } else {
                    LOGGER.log(Level.INFO, "SessionFailedResponse with general server failure");
                }
                collar.changeState(State.DISCONNECTED);
            } else if (resp instanceof IsTrustedRelationshipResponse) {
                LOGGER.log(Level.INFO, "Server has confirmed a trusted relationship with the client");
                this.serverIdentity = resp.identity;
                collar.changeState(State.CONNECTED);
            } else if (resp instanceof IsUntrustedRelationshipResponse) {
                LOGGER.log(Level.INFO, "Server has declared the client as untrusted. Consumer should reset the identity store and reconnect.");
                collar.changeState(State.DISCONNECTED);
                configuration.listener.onClientUntrusted(collar, identityStore);
            } else {
                for (AbstractApi<?> api : collar.apis) {
                    if (api.handleResponse(resp)) {
                        break;
                    }
                }
            }
        }

        private ProtocolResponse readResponse(byte[] bytes) {
            PacketIO packetIO = new PacketIO(mapper, identityStore == null ? null : identityStore.createCypher());
            try {
                return packetIO.decode(serverIdentity, bytes, ProtocolResponse.class);
            } catch (IOException e) {
                throw new IllegalStateException("Read error ", e);
            }
        }

        public void sendRequest(WebSocket webSocket, ProtocolRequest req) {
            PacketIO packetIO = identityStore == null ? new PacketIO(mapper, null) : new PacketIO(mapper, identityStore.createCypher());
            byte[] bytes;
            if (state == State.CONNECTED) {
                try {
                    if (identityStore == null) {
                        throw new IllegalStateException("identity store should be available by the time the client is CONNECTED");
                    }
                    bytes = packetIO.encodeEncrypted(serverIdentity, req);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                try {
                    bytes = packetIO.encodePlain(req);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            webSocket.send(ByteString.of(bytes));
        }
    }

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
