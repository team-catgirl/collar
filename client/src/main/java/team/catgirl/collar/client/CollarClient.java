package team.catgirl.collar.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.client.security.signal.SignalClientIdentityStore;
import team.catgirl.collar.messages.ClientMessage;
import team.catgirl.collar.messages.ClientMessage.*;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.messages.ServerMessage.*;
import team.catgirl.collar.models.Group;
import team.catgirl.collar.models.Group.MembershipState;
import team.catgirl.collar.models.Position;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CollarClient {
    private static final Logger LOGGER = Logger.getLogger(CollarClient.class.getName());

    private final ObjectMapper mapper = Utils.createObjectMapper();
    private final String baseUrl;
    private final OkHttpClient http;
    private final HomeDirectory homeDirectory;
    private ClientIdentityStore identityStore;
    private ServerIdentity serverIdentity;
    private WebSocket webSocket;
    private DelegatingListener listener;
    private ClientState state = ClientState.DISCONNECTED;
    private ScheduledExecutorService keepAliveScheduler;
    private CreateIdentityRequest createIdentityRequest;

    public CollarClient(String baseUrl, HomeDirectory homeDirectory) {
        this.homeDirectory = homeDirectory;
        if (Strings.isNullOrEmpty(baseUrl)) {
            throw new IllegalArgumentException("baseUrl was not set");
        }
        // Make sure we don't append the / twice otherwise jetty won't connect to server
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
        }
        this.baseUrl = baseUrl + "/api/1/";
        this.http = new OkHttpClient();
    }

    public void connect(UUID player, CollarListener listener) throws IOException {
        // Do not run if we are already connected
        if (state != ClientState.DISCONNECTED) {
            throw new IllegalStateException("Client is in state " + state);
        }
        state = ClientState.CONNECTING;
        // Setup the identity store for this player
        this.identityStore = SignalClientIdentityStore.from(player, homeDirectory, (signedPreKeyRecord, preKeyRecords) -> {
            createIdentityRequest = CreateIdentityRequest.from(signedPreKeyRecord, preKeyRecords);
        });
        this.listener = new DelegatingListener(listener);
        Request request = new Request.Builder().url(baseUrl + "listen").build();
        webSocket = http.newWebSocket(request, new WebSocketListenerImpl(this));
        http.dispatcher().executorService().shutdown();
        startKeepAlive();
    }

    public void disconnect() {
        if (state == ClientState.DISCONNECTED) {
            throw new IllegalStateException("Client is in state " + state);
        }
        webSocket.close(1000, "Collar was disconnected by the client");
        CollarListener listener = this.listener;
        this.listener = null;
        this.identityStore = null;
        this.serverIdentity = null;
        stopKeepAlive();
        this.state = ClientState.DISCONNECTED;
        listener.onDisconnect(this);
    }

    public ClientState getState() {
        return state;
    }

    public boolean isServerAvailable() {
        Request request = new Request.Builder()
                .url(baseUrl)
                .build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 200;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * @return the players identity or null if not identified
     */
    public PlayerIdentity identity() {
        return identityStore != null ? identityStore.currentIdentity() : null;
    }

    public void createGroup(List<UUID> players, Position position) throws IOException {
        CreateGroupRequest req = new CreateGroupRequest(players, position);
        send(req.clientMessage(identity()));
    }

    public void acceptGroupRequest(String groupId, MembershipState state) throws IOException {
        send(new AcceptGroupMembershipRequest(groupId, state).clientMessage(identity()));
    }

    public void leaveGroup(Group group) throws IOException {
        send(new LeaveGroupRequest(group.id).clientMessage(identity()));
    }

    public void updatePosition(Position position) throws IOException {
        send(new UpdatePlayerStateRequest(position).clientMessage(identity()));
    }

    public void invite(Group group, List<UUID> players) throws IOException {
        send(new GroupInviteRequest(group.id, players).clientMessage(identity()));
    }

    private void send(ClientMessage o) throws IOException {
        String message = mapper.writeValueAsString(o);
        if (state == ClientState.CONNECTED) {
            byte[] bytes = identityStore.createCypher().crypt(serverIdentity, message.getBytes(StandardCharsets.UTF_8));
            webSocket.send(BaseEncoding.base64().encode(bytes));
        } else {
            // End non-encrypted message
            webSocket.send(message);
        }
    }

    private void startKeepAlive() {
        keepAliveScheduler = Executors.newScheduledThreadPool(1);
        keepAliveScheduler.scheduleAtFixedRate(() -> {
            try {
                send(new Ping().clientMessage(identity()));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Couldn't send ping");
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void stopKeepAlive() {
        keepAliveScheduler.shutdown();
        keepAliveScheduler = null;
    }

    class WebSocketListenerImpl extends WebSocketListener {

        private final CollarClient client;

        public WebSocketListenerImpl(CollarClient client) {
            this.client = client;
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            LOGGER.info("onOpen is called");
            super.onOpen(webSocket, response);
            ClientMessage message;
            if (createIdentityRequest == null) {
                // This is an exiting client installation
                message = new IdentifyRequest().clientMessage(identity());
            } else {
                // This is a brand new client installation
                message = createIdentityRequest.clientMessage(identity());
            }
            try {
                send(message);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not send. Closing client", e);
                disconnect();
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            ServerMessage message;
            try {
                // If the client is connected we are speaking signal
                if (state == ClientState.CONNECTED) {
                    byte[] bytes = identityStore.createCypher().decrypt(client.identity(), BaseEncoding.base64().decode(text));
                    message = mapper.readValue(bytes, ServerMessage.class);
                } else {
                    // Otherwise we are communicating in plain text
                    message = mapper.readValue(text, ServerMessage.class);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Couldn't parse server message", e);
                disconnect();
                return;
            }
            if (message.serverConnectedResponse != null) {
                listener.onConnected(client, message.identity);
            }
            if (message.identificationResponse != null) {
                if (identityStore.isTrustedIdentity(message.identity)) {
                    listener.onSessionCreated(client);
                } else {
                    identityStore.trustIdentity(message.identity);
                    listener.onSessionCreated(client);
                }
                serverIdentity = message.identity;
            }
            if (message.groupMembershipRequest != null) {
                listener.onGroupMembershipRequested(client, message.groupMembershipRequest);
            }
            if (message.createGroupResponse != null) {
                listener.onGroupCreated(client, message.createGroupResponse);
            }
            if (message.leaveGroupResponse != null) {
                listener.onGroupLeft(client, message.leaveGroupResponse);
            }
            if (message.updatePlayerStateResponse != null) {
                listener.onGroupUpdated(client, message.updatePlayerStateResponse);
            }
            if (message.groupInviteResponse != null) {
                listener.onGroupInvitesSent(client, message.groupInviteResponse);
            }
            if (message.acceptGroupMembershipResponse != null) {
                listener.onGroupJoined(client, message.acceptGroupMembershipResponse);
            }
            if (message.pong != null) {
                listener.onPongReceived(message.pong);
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            LOGGER.log(Level.INFO, "Connection closing...");
            if (state != ClientState.DISCONNECTED) {
                disconnect();
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            LOGGER.log(Level.SEVERE, "Communications failure", t);
            disconnect();
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            LOGGER.log(Level.SEVERE, "closed: " + reason);
        }
    }

    public class DelegatingListener implements CollarListener {
        private final CollarListener listener;

        public DelegatingListener(CollarListener listener) {
            this.listener = listener;
        }

        @Override
        public void onConnected(CollarClient client, ServerIdentity reportedServerIdentity) {
            serverIdentity = reportedServerIdentity;
            listener.onConnected(client, reportedServerIdentity);
        }

        @Override
        public void onSessionCreated(CollarClient client) {
            state = ClientState.CONNECTED;
            listener.onSessionCreated(client);
        }

        @Override
        public void onDisconnect(CollarClient client) {
            listener.onDisconnect(client);
        }

        @Override
        public void onGroupCreated(CollarClient client, CreateGroupResponse resp) {
            listener.onGroupCreated(client, resp);
        }

        @Override
        public void onGroupMembershipRequested(CollarClient client, GroupMembershipRequest resp) {
            listener.onGroupMembershipRequested(client, resp);
        }

        @Override
        public void onGroupJoined(CollarClient client, AcceptGroupMembershipResponse acceptGroupMembershipResponse) {
            listener.onGroupJoined(client, acceptGroupMembershipResponse);
        }

        @Override
        public void onGroupLeft(CollarClient client, LeaveGroupResponse resp) {
            listener.onGroupLeft(client, resp);
        }

        @Override
        public void onGroupUpdated(CollarClient client, UpdatePlayerStateResponse updatePlayerStateResponse) {
            listener.onGroupUpdated(client, updatePlayerStateResponse);
        }

        @Override
        public void onPongReceived(ServerMessage.Pong pong) {
            listener.onPongReceived(pong);
        }

        @Override
        public void onGroupInvitesSent(CollarClient client, GroupInviteResponse resp) {
            listener.onGroupInvitesSent(client, resp);
        }
    }

    public enum ClientState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
