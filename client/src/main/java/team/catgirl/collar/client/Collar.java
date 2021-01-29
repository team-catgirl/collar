package team.catgirl.collar.client;

import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.catgirl.collar.client.CollarClientException.ConnectionException;
import team.catgirl.collar.client.CollarClientException.UnsupportedVersionException;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.http.ServerStatusResponse;
import team.catgirl.collar.messages.ClientMessage;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

public final class Collar {
    private static final int VERSION = 1;

    private final UUID playerId;
    private final String baseUrl;
    private final HomeDirectory home;
    private final OkHttpClient http;
    private final WebSocket webSocket;

    private Collar(OkHttpClient http, UUID playerId, String baseUrl, HomeDirectory home) {
        this.http = http;
        this.playerId = playerId;
        this.baseUrl = baseUrl;
        this.home = home;
        webSocket = http.newWebSocket(new Request.Builder().url(baseUrl + "listen").build(), new WebSocketListenerImpl());
    }

    /**
     * Create a new Collar client
     * @param playerId of the minecraft player
     * @param baseUrl of the collar server
     * @param minecraftHome the minecraft data directory
     * @return collar client
     * @throws IOException setting up
     */
    public static Collar create(UUID playerId, String baseUrl, File minecraftHome) throws IOException {
        OkHttpClient http = new OkHttpClient();
        checkVersionCompatibility(http);
        checkApiStatus(http, baseUrl);
        return new Collar(http, playerId, baseUrl, HomeDirectory.from(minecraftHome, playerId));
    }

    /**
     * Test that the client version is supported by the server
     * @param http client
     */
    private static void checkVersionCompatibility(OkHttpClient http) {
        int[] supportedVersions = httpGet(http, "/discover", int[].class);
        if (Arrays.stream(supportedVersions).noneMatch(value -> value == VERSION)) {
            StringJoiner versions = new StringJoiner(",");
            Arrays.stream(supportedVersions).forEach(value -> versions.add(String.valueOf(value)));
            throw new UnsupportedVersionException("version " + VERSION + " is not supported by server. Server supports versions " + versions.toString());
        }
    }

    /**
     * Test that the API is available and OK
     * @param http client
     * @param baseUrl collar server
     */
    private static void checkApiStatus(OkHttpClient http, String baseUrl) {
        ServerStatusResponse resp = httpGet(http, buildUrl(baseUrl, "status"), ServerStatusResponse.class);
        if (!resp.status.equals("OK")) {
            throw new ConnectionException("Server resp was " + resp.status);
        }
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

    /**
     * Builds a versioned collar API url
     * @param baseUrl of the server
     * @param url of the collar api endpoint
     * @return the full url
     */
    private static String buildUrl(String baseUrl, String url) {
        return baseUrl + "/" + VERSION + "/" + url;
    }

    private class WebSocketListenerImpl extends WebSocketListener {
        private ClientIdentityStore identityStore;

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
//            this.identityStore = SignalClientIdentityStore.from(playerId, home, (signalProtocolStore, bundle) -> {
//                PreKeyBundle preKeyBundle = PreKeys.generate(store, 1);
//                RegisterClientRequest req = new RegisterClientRequest(PreKeys.preKeyBundleToBytes(bundle));
//                webSocket.send();
//            });
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
        public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
            super.onMessage(webSocket, bytes);
        }

        private void sendPlain(WebSocket webSocket, ClientMessage message) {

        }
    }
}
