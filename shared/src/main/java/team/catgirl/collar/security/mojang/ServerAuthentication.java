package team.catgirl.collar.security.mojang;

import okhttp3.Request;
import okhttp3.Response;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class ServerAuthentication {

    private final String CLIENT_JOIN_URL = "https://sessionserver.mojang.com/session/minecraft/join";

    public static final class JoinRequest {
        public final String accessToken;
        public final String selectedProfile;
        public final String serverId;

        public JoinRequest(String accessToken, UUID playerId, String serverId) {
            this.accessToken = accessToken;
            this.selectedProfile = playerId.toString().replace("-", "");
            this.serverId = serverId;
        }
    }

    /**
     * Creates the non-standard Mojang sha1 hash
     * @param str
     * @return hash
     * @throws NoSuchAlgorithmException
     */
    private static String hash(String str) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] digest = md.digest(strBytes);
        return new BigInteger(digest).toString(16);
    }

    private static <T> T httpPost(String url, Class<T> aClass) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = Utils.http().newCall(request).execute()) {
            if (response.code() == 200) {
                byte[] bytes = Objects.requireNonNull(response.body()).bytes();
                return Utils.jsonMapper().readValue(bytes, aClass);
            } else {
                throw new IOException("Failed to connect to server");
            }
        } catch (IOException e) {
            throw new IOException("Failed to connect to server", e);
        }
    }
}
