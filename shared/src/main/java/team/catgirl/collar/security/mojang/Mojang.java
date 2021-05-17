package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.io.BaseEncoding;
import io.mikael.urlbuilder.UrlBuilder;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.http.Request;
import team.catgirl.collar.http.Response;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class Mojang {

    private static final Logger LOGGER = Logger.getLogger(Mojang.class.getName());

    private final HttpClient http;
    private final String baseUrl;

    public Mojang(HttpClient http, String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl;
    }

    public PlayerProfile getProfile(UUID id) {
        String profileId = id.toString().replace("-", "");
        return http.execute(Request.url(baseUrl + "session/minecraft/profile/" + profileId).get(), Response.json(PlayerProfile.class));
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
                    .addParameter("serverId", Mojang.genServerIDHash());
            HasJoinedResponse hasJoinedResponse = http.execute(Request.url(builder).get(), Response.json(HasJoinedResponse.class));
            return hasJoinedResponse.id.equals(toProfileId(session.id));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Couldn't verify " + session.username,e);
            return false;
        }
    }

    public static final class PlayerProfile {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        public final String name;
        @JsonProperty("properties")
        public final List<ProfileProperty> properties;

        public PlayerProfile(@JsonProperty("id") String id,
                             @JsonProperty("name") String name,
                             @JsonProperty("properties") List<ProfileProperty> properties) {
            this.id = id;
            this.name = name;
            this.properties = properties;
        }

        public Optional<TexturesProperty> textures() {
            return properties.stream().filter(profileProperty -> profileProperty.name.equals("textures"))
                    .map(profileProperty -> profileProperty.value)
                    .map(value -> BaseEncoding.base64().decode(value))
                    .map(bytes -> {
                        try {
                            return Utils.jsonMapper().readValue(bytes, TexturesProperty.class);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .findFirst();
        }
    }

    public static final class ProfileProperty {
        @JsonProperty("name")
        public final String name;
        @JsonProperty("value")
        public final String value;

        public ProfileProperty(@JsonProperty("name") String name,
                               @JsonProperty("value") String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static final class TexturesProperty {
        @JsonProperty("timestamp")
        public final long timestamp;
        @JsonProperty("profileId")
        public final String profileId;
        @JsonProperty("profileName")
        public final String profileName;
        public final Map<String, Texture> textures;

        public TexturesProperty(@JsonProperty("timestamp") long timestamp,
                                @JsonProperty("profileId") String profileId,
                                @JsonProperty("profileName") String profileName,
                                @JsonProperty("textures") Map<String, Texture> textures) {
            this.timestamp = timestamp;
            this.profileId = profileId;
            this.profileName = profileName;
            this.textures = textures;
        }

        public Optional<String> skin() {
            return getTexture("SKIN");
        }

        public Optional<String> cape() {
            return getTexture("CAPE");
        }

        private Optional<String> getTexture(String name) {
            Texture texture = textures.get(name);
            return texture == null ? Optional.empty() : Optional.of(texture.url);
        }
    }

    public static final class Texture {
        @JsonProperty("url")
        public final String url;

        public Texture(@JsonProperty("url") String url) {
            this.url = url;
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
