package team.catgirl.collar.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.whispersystems.libsignal.IdentityKey;
import team.catgirl.collar.client.CollarClientException.ConnectionException;
import team.catgirl.collar.client.CollarClientException.UnsupportedVersionException;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.client.security.signal.SignalClientIdentityStore;
import team.catgirl.collar.protocol.*;
import team.catgirl.collar.security.KeyPair.PublicKey;
import team.catgirl.collar.security.PlayerIdentity;
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
    private final OkHttpClient http;
    private final WebSocket webSocket;

    private Collar(OkHttpClient http, UUID playerId, UrlBuilder baseUrl, HomeDirectory home) {
        this.http = http;
        this.playerId = playerId;
        this.baseUrl = baseUrl;
        this.home = home;
        String url = baseUrl.withPath("/api/1/listen").toString();
        LOGGER.log(Level.INFO, "Connecting to server " + url);
        webSocket = http.newWebSocket(new Request.Builder().url(url).build(), new CollarWebSocket());
        webSocket.request();
        http.dispatcher().executorService().shutdown();
    }

    /**
     * Create a new Collar client
     * @param playerId of the minecraft player
     * @param server address of the collar server
     * @param minecraftHome the minecraft data directory
     * @return collar client
     * @throws IOException setting up
     */
    public static Collar create(UUID playerId, String server, File minecraftHome) throws IOException {
        OkHttpClient http = new OkHttpClient();
        UrlBuilder baseUrl = UrlBuilder.fromString(server);
        checkVersionCompatibility(http, baseUrl);
        return new Collar(http, playerId, baseUrl, HomeDirectory.from(minecraftHome, playerId));
    }

    /**
     * Test that the client version is supported by the server
     * @param http client
     */
    private static void checkVersionCompatibility(OkHttpClient http, UrlBuilder baseUrl) {
        int[] supportedVersions = httpGet(http, baseUrl.withPath("/api/discover").toString(), int[].class);
        if (Arrays.stream(supportedVersions).noneMatch(value -> value == VERSION)) {
            StringJoiner versions = new StringJoiner(",");
            Arrays.stream(supportedVersions).forEach(value -> versions.add(String.valueOf(value)));
            throw new UnsupportedVersionException("version " + VERSION + " is not supported by server. Server supports versions " + versions.toString());
        }
        LOGGER.log(Level.INFO, "Server supports versions " + supportedVersions);
    }

    private static  <T> T httpGet(OkHttpClient http, String url, Class<T> aClass) {
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

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            LOGGER.log(Level.INFO, "Connection established");
            try {
                this.identityStore = SignalClientIdentityStore.from(playerId, home, signalProtocolStore -> {
                    LOGGER.log(Level.INFO, "New installation. Registering device with server...");
                    IdentityKey publicKey = signalProtocolStore.getIdentityKeyPair().getPublicKey();
                    PlayerIdentity serverIdentity = new PlayerIdentity(playerId, new PublicKey(publicKey.getFingerprint(), publicKey.serialize()));
                    RegisterDeviceRequest request = new RegisterDeviceRequest(serverIdentity);
                    send(webSocket, request);
                });
            } catch (IOException e) {
                throw new IllegalStateException("could not setup identity store", e);
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String message) {
            LOGGER.log(Level.INFO, "Message received " + message);
            ProtocolResponse resp;
            try {
                resp = mapper.readValue(message, ProtocolResponse.class);
            } catch (IOException e) {
                throw new ConnectionException("Could not read message", e);
            }
            if (resp instanceof RegisterDeviceResponse) {
                RegisterDeviceResponse registerDeviceResponse = (RegisterDeviceResponse)resp;
                LOGGER.log(Level.INFO, "RegisterDeviceResponse received with registration url " + ((RegisterDeviceResponse) resp).approvalUrl);
            } else if (resp instanceof DeviceRegisteredResponse) {
                DeviceRegisteredResponse response = (DeviceRegisteredResponse)resp;
                LOGGER.log(Level.INFO, "Ready to exchange keys for device " + response.deviceId);
            }
        }

        void send(WebSocket webSocket, ProtocolRequest req) {
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
