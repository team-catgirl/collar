package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.mikael.urlbuilder.UrlBuilder;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.http.Request;
import team.catgirl.collar.http.Response;

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

    private final HttpClient http;
    private final String baseUrl;

    public ServerAuthentication(HttpClient http, String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl;
    }

    public boolean joinServer(MinecraftSession session) {
        try {
            JoinRequest joinReq = new JoinRequest(session.accessToken, toProfileId(session.id), session.server);
            http.execute(Request.url(baseUrl + "session/minecraft/join").post(joinReq), Response.noContent());
            return true;
        } catch (HttpException e) {
            LOGGER.log(Level.SEVERE, "Could not start verification with Mojang",e);
            return false;
        }
    }

    public boolean verifyClient(MinecraftSession session) {
        try {
            UrlBuilder builder = UrlBuilder.fromString(baseUrl + "session/minecraft/hasJoined")
                    .addParameter("username", session.username)
                    .addParameter("serverId", ServerAuthentication.genServerIDHash());
            HasJoinedResponse hasJoinedResponse = http.execute(Request.url(builder).get(), Response.json(HasJoinedResponse.class));
            return hasJoinedResponse.id.equals(toProfileId(session.id));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Couldn't verify " + session.username,e);
            return false;
        }
    }

    public static final class JoinRequest {
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("selectedProfile")
        public final String selectedProfile;
        @JsonProperty("serverId")
        public final String serverId;

        @JsonCreator
        public JoinRequest(@JsonProperty("accessToken") String accessToken,
                           @JsonProperty("selectedProfile") String selectedProfile,
                           @JsonProperty("serverId") String serverId) {
            this.accessToken = accessToken;
            this.selectedProfile = selectedProfile;
            this.serverId = serverId;
        }
    }

    public static final class HasJoinedResponse {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        public final String name;

        @JsonCreator
        public HasJoinedResponse(@JsonProperty("id") String id,
                                 @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
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
