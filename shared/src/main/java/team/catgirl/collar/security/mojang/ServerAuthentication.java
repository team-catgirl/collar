package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.databind.JsonNode;
import io.mikael.urlbuilder.UrlBuilder;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerAuthentication {

    private static final Logger LOGGER = Logger.getLogger(ServerAuthentication.class.getName());

    private final String baseUrl;

    public ServerAuthentication(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean joinServer(MinecraftSession session) {
        try {
            JoinRequest joinReq = new JoinRequest(session.accessToken, session.id);
            byte[] json = Utils.jsonMapper().writeValueAsBytes(joinReq);
            RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .url(baseUrl + "/session/minecraft/join")
                    .post(body)
                    .build();
            try (Response response = Utils.http().newCall(request).execute()) {
                if (response.code() != 204) {
                    LOGGER.log(Level.SEVERE, "Couldn't register!\n"+response.body().string());
                }
                return response.code() == 204;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Couldn't register",e);
                return false;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Couldn't register",e);
            return false;
        }
    }

    public boolean verifyClient(MinecraftSession session) {
        try {
            UrlBuilder builder = UrlBuilder.fromString(baseUrl)
                    .withPath("/session/minecraft/hasJoined")
                    .addParameter("username", session.username)
                    .addParameter("serverId", ServerAuthentication.genServerIDHash());
            Request request = new Request.Builder().url(builder.toUrl()).build();
            try (Response response = Utils.http().newCall(request).execute()) {
                JsonNode resp = Utils.jsonMapper().readTree(response.body().string());
                if (!resp.has("id")) {
                    LOGGER.log(Level.SEVERE, "Couldn't verify " + session.username.toUpperCase());
                    return false;
                }
                return response.code() == 200 && resp.get("id").asText().equals(toProfileId(session.id));
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Couldn't verify " + session.username.toUpperCase(),e);
            return false;
        }
    }

    public static final class JoinRequest {
        public final String accessToken;
        public final String selectedProfile;
        public final String serverId;

        public JoinRequest(String accessToken, UUID playerId, String serverId) {
            this.accessToken = accessToken;
            this.selectedProfile = toProfileId(playerId);
            this.serverId = serverId;
        }

        public JoinRequest(String accessToken, UUID id) throws NoSuchAlgorithmException {
            this.accessToken = accessToken;
            this.selectedProfile = toProfileId(id);
            this.serverId = genServerIDHash();
        }
    }

    private static String genServerIDHash() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(generateRandomKey());
        byte[] digest = md.digest();
        return new BigInteger(digest).toString(16);
    }

    private static String toProfileId(UUID id) {
        return id.toString().replace("-", "");
    }

    private static byte[] generateRandomKey() {
        KeyPair kp;
        try {
            KeyPairGenerator keyPairGene = KeyPairGenerator.getInstance("RSA");
            keyPairGene.initialize(512);
            kp = keyPairGene.genKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("problem generating the key", e);
        }
        return kp.getPublic().getEncoded();
    }
}
