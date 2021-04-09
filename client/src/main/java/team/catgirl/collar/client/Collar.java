package team.catgirl.collar.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import org.whispersystems.libsignal.IdentityKey;
import team.catgirl.collar.api.http.CollarFeature;
import team.catgirl.collar.api.http.CollarVersion;
import team.catgirl.collar.api.http.DiscoverResponse;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.session.Player;
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
import team.catgirl.collar.client.utils.Http;
import team.catgirl.collar.http.Request;
import team.catgirl.collar.http.Response;
import team.catgirl.collar.http.WebSocket;
import team.catgirl.collar.http.WebSocketListener;
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
import team.catgirl.collar.protocol.session.SessionFailedResponse.PrivateIdentityMismatchResponse;
import team.catgirl.collar.protocol.session.SessionFailedResponse.SessionErrorResponse;
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
import team.catgirl.collar.security.mojang.MinecraftSession;
import team.catgirl.collar.security.mojang.ServerAuthentication;
import team.catgirl.collar.client.utils.Crypto;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static team.catgirl.collar.http.Request.url;

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

    private Collar(CollarConfiguration configuration) throws IOException {
        this.configuration = configuration;
        changeState(State.DISCONNECTED);
        this.identityStoreSupplier = () -> identityStore;
        Consumer<ProtocolRequest> sender = request -> this.sender.accept(request);
        this.ticks = configuration.ticks;
        this.recordCiphers = new ContentCiphers();
        this.apis = new ArrayList<>();
        this.sdhtApi = new SDHTApi(this, identityStoreSupplier, sender, recordCiphers, this.ticks, this.configuration.homeDirectory.dhtState());
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
        try {
            return new Collar(configuration);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Connect to server
     */
    public void connect() {
        checkServerCompatibility(configuration);
        String url = UrlBuilder.fromUrl(configuration.collarServerURL).withPath("/api/1/listen").toString();
        LOGGER.log(Level.INFO, "Connecting to server " + url);
        webSocket = Http.collar().webSocket(Request.url(url).ws(), new CollarWebSocket(this));
        changeState(State.CONNECTING);
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        if (this.webSocket != null) {
            LOGGER.log(Level.INFO, "Disconnected");
            this.webSocket.close();
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
        DiscoverResponse response;
        try {
            response = Http.collar().execute(url(UrlBuilder.fromUrl(configuration.collarServerURL).withPath("/api/discover")).get(), Response.json(DiscoverResponse.class));
        } catch (HttpException e) {
            throw new ConnectionException("Problem connecting to collar", e);
        }
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
            if ("mojang".equals(collarFeature.value) && minecraftSession.mode != MinecraftSession.Mode.MOJANG && minecraftSession.accessToken == null) {
                throw new IllegalStateException("mojang verification scheme s but was provided an invalid MinecraftSession");
            } else if ("nojang".equals(collarFeature.value) && minecraftSession.mode != MinecraftSession.Mode.NOJANG && minecraftSession.accessToken != null) {
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

    /**
     * The current player
     * @return player
     */
    public Player player() {
        return new Player(identity().id(), configuration.sessionSupplier.get().toPlayer());
    }

    private void assertConnected() {
        if (state != State.CONNECTED) {
            throw new IllegalStateException("Cannot use the API until client is CONNECTED");
        }
    }

    class CollarWebSocket implements WebSocketListener {
        private final ObjectMapper mapper = Utils.messagePackMapper();
        private final Collar collar;
        private KeepAlive keepAlive;
        private volatile ServerIdentity serverIdentity;

        public CollarWebSocket(Collar collar) {
            this.collar = collar;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
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
                PrivateIdentity privateIdentity = PrivateIdentity.getOrCreate(configuration.homeDirectory);
                IdentifyRequest request = new IdentifyRequest(clientIdentity, privateIdentity.token);
                sendRequest(webSocket, request);
            }));
        }

        @Override
        public void onClose(WebSocket webSocket, int code, String message) {
            LOGGER.log(Level.SEVERE, "Closed socket: " + message);
            if (this.keepAlive != null) {
                this.keepAlive.stop();
            }
            if (state != State.DISCONNECTED) {
                collar.changeState(State.DISCONNECTED);
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable) {
            LOGGER.log(Level.SEVERE, "Socket failure", throwable);
            throwable.printStackTrace();
            if (state != State.DISCONNECTED) {
                collar.changeState(State.DISCONNECTED);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteBuffer messageBuffer) {
            ProtocolResponse resp = readResponse(messageBuffer);
            LOGGER.log(Level.INFO, resp.getClass().getSimpleName() + " from " + resp.identity);
            ClientIdentity identity = identityStore == null ? null : identityStore.currentIdentity();
            if (resp instanceof IdentifyResponse) {
                IdentifyResponse response = (IdentifyResponse) resp;
                if (identityStore == null) {
                    identityStore = getOrCreateIdentityKeyStore(webSocket, response.profile.id);
                }
                MinecraftSession session = configuration.sessionSupplier.get();
                if (session.mode == MinecraftSession.Mode.MOJANG) {
                    ServerAuthentication authentication = new ServerAuthentication(Http.external(), configuration.yggdrasilBaseUrl);
                    if (!authentication.joinServer(session)) {
                        throw new ConnectionException("could start session with Mojang");
                    }
                }
                StartSessionRequest request = new StartSessionRequest(identity, session);
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
                sendRequest(webSocket, new IdentifyRequest(identity, identityStore.privateIdentityToken()));
            } else if (resp instanceof StartSessionResponse) {
                LOGGER.log(Level.INFO, "Session has started. Checking if the client and server are in a trusted relationship");
                sendRequest(webSocket, new CheckTrustRelationshipRequest(identity));
            } else if (resp instanceof SessionFailedResponse) {
                LOGGER.log(Level.INFO, "SessionFailedResponse received");
                if (resp instanceof MojangVerificationFailedResponse) {
                    MojangVerificationFailedResponse response = (MojangVerificationFailedResponse) resp;
                    LOGGER.log(Level.INFO, "SessionFailedResponse with mojang session verification failure");
                    configuration.listener.onMinecraftAccountVerificationFailed(collar, response.minecraftSession);
                } else if (resp instanceof PrivateIdentityMismatchResponse) {
                    PrivateIdentityMismatchResponse response = (PrivateIdentityMismatchResponse) resp;
                    LOGGER.log(Level.INFO, "SessionFailedResponse with private identity mismatch");
                    configuration.listener.onPrivateIdentityMismatch(collar, response.url);
                } else if (resp instanceof SessionErrorResponse) {
                    LOGGER.log(Level.INFO, "SessionFailedResponse Reason: " + ((SessionErrorResponse) resp).reason);
                }
                collar.changeState(State.DISCONNECTED);
            } else if (resp instanceof IsTrustedRelationshipResponse) {
                LOGGER.log(Level.INFO, "Server has confirmed a trusted relationship with the client");
                if (resp.identity == null) {
                    throw new IllegalStateException("sever identity was null");
                }
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

        private ProtocolResponse readResponse(ByteBuffer buffer) {
            PacketIO packetIO = new PacketIO(mapper, identityStore == null ? null : identityStore.createCypher());
            try {
                return packetIO.decode(serverIdentity, buffer, ProtocolResponse.class);
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
            webSocket.send(ByteBuffer.wrap(bytes));
        }
    }

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
;
    static {
        Crypto.removeCryptographyRestrictions();
    }
}
