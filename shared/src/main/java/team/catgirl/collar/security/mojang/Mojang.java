package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;
import com.google.common.io.BaseEncoding;
import io.mikael.urlbuilder.UrlBuilder;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.http.Request;
import team.catgirl.collar.http.Response;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Mojang {

    private static final Logger LOGGER = Logger.getLogger(Mojang.class.getName());

    public static final String DEFAULT_AUTH_SERVER = "https://authserver.mojang.com/";
    public static final String DEFAULT_SESSION_SERVER = "https://sessionserver.mojang.com/";

    private static final Supplier<KeyPair> SERVER_KEY_PAIR = Suppliers.memoize(Mojang::createKeyPair);

    private final HttpClient http;
    private final String sessionServerBaseUrl;
    private final String authServerBaseUrl;

    public Mojang(HttpClient http, String sessionServerBaseUrl, String authServerBaseUrl) {
        this.http = http;
        this.sessionServerBaseUrl = sessionServerBaseUrl;
        this.authServerBaseUrl = authServerBaseUrl;
    }

    public Mojang(HttpClient client) {
        this(client, Mojang.DEFAULT_SESSION_SERVER, Mojang.DEFAULT_AUTH_SERVER);
    }

    public PlayerPlayerProfile getProfile(UUID id) {
        String profileId = id.toString().replace("-", "");
        return http.execute(Request.url(sessionServerBaseUrl + "session/minecraft/profile/" + profileId).get(), Response.json(PlayerPlayerProfile.class));
    }

    /**
     * Join
     * @param session
     * @param serverPublicKey
     * @param sharedSecret
     * @return
     */
    public Optional<JoinServerResponse> joinServer(MinecraftSession session, String serverPublicKey, byte[] sharedSecret) {
        try {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            md.update("".getBytes(StandardCharsets.ISO_8859_1));
            md.update(sharedSecret);
            md.update(serverPublicKey.getBytes(StandardCharsets.ISO_8859_1));
            byte[] digest = md.digest();
            String serverId = new BigInteger(digest).toString(16);
            JoinRequest joinReq = new JoinRequest(Agent.MINECRAFT, session.accessToken, toProfileId(session.id), serverId);
            http.execute(Request.url(sessionServerBaseUrl + "session/minecraft/join").postJson(joinReq), Response.noContent());
            return Optional.of(new JoinServerResponse(serverId));
        } catch (HttpException e) {
            LOGGER.log(Level.SEVERE, "Could not start verification with Mojang (" + e.code + ")", e);
            return Optional.empty();
        }
    }

    /**
     * Verify that the client can login to the server
     * @param session to check
     * @param serverId server id
     * @return client verified or not
     */
    public boolean hasJoined(MinecraftSession session, String serverId) {
        try {
            UrlBuilder builder = UrlBuilder.fromString(sessionServerBaseUrl + "session/minecraft/hasJoined")
                    .addParameter("username", session.username)
                    .addParameter("serverId", serverId);
            HasJoinedResponse hasJoinedResponse = http.execute(Request.url(builder).get(), Response.json(HasJoinedResponse.class));
            return hasJoinedResponse.id.equals(toProfileId(session.id));
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Couldn't verify " + session.username,e);
            return false;
        }
    }

    /**
     * Validates the clients access token
     * @param request to send
     * @return accessToken is valid or not
     */
    public boolean validateToken(ValidateTokenRequest request) {
        try {
            http.execute(Request.url(authServerBaseUrl + "/validate").postJson(request), Response.noContent());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Validates the clients access token
     * @param request to send
     * @return accessToken is valid or not
     */
    public Optional<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {
        try {
            return Optional.of(http.execute(Request.url(authServerBaseUrl + "/refresh").postJson(request), Response.json(RefreshTokenResponse.class)));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    /**
     * Authenticate with Mojang
     * @param request to send
     * @return response on success or empty on failure
     */
    public Optional<AuthenticateResponse> authenticate(AuthenticateRequest request) {
        try {
            return Optional.of(http.execute(Request.url(authServerBaseUrl + "/authenticate").postJson(request), Response.json(AuthenticateResponse.class)));
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "auth failed " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    public static final class RefreshTokenRequest {
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("clientToken")
        public final UUID clientToken;
        @JsonProperty("selectedProfile")
        public final PlayerProfileInfo selectedProfile;
        @JsonProperty("requestUser")
        public final boolean requestUser;

        public RefreshTokenRequest(@JsonProperty("accessToken") String accessToken,
                                   @JsonProperty("clientToken") UUID clientToken,
                                   @JsonProperty("selectedProfile") PlayerProfileInfo selectedProfile,
                                   @JsonProperty("requestUser") boolean requestUser) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
            this.selectedProfile = selectedProfile;
            this.requestUser = requestUser;
        }
    }

    public static final class RefreshTokenResponse {
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("clientToken")
        public final String clientToken;
        @JsonProperty("selectedProfile")
        public final PlayerProfileInfo selectedProfile;
        @JsonProperty("user")
        public final PlayerPlayerProfile user;

        public RefreshTokenResponse(@JsonProperty("accessToken") String accessToken,
                                    @JsonProperty("clientToken") String clientToken,
                                    @JsonProperty("selectedProfile") PlayerProfileInfo selectedProfile,
                                    @JsonProperty("user") PlayerPlayerProfile user) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
            this.selectedProfile = selectedProfile;
            this.user = user;
        }
    }

    public static class PlayerProfileInfo {
        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        public final String name;

        public PlayerProfileInfo(@JsonProperty("id") String id,
                                 @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }

        public UUID toId() {
            return UUID.fromString(id.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5" ));
        }
    }

    public static final class ValidateTokenRequest {
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("clientToken")
        public final UUID clientToken;

        public ValidateTokenRequest(@JsonProperty("accessToken") String accessToken,
                                    @JsonProperty("clientToken") UUID clientToken) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
        }
    }

    public static final class PlayerPlayerProfile extends PlayerProfileInfo {
        @JsonProperty("properties")
        public final List<PlayerProfileProperty> properties;

        public PlayerPlayerProfile(@JsonProperty("id") String id,
                                   @JsonProperty("name") String name,
                                   @JsonProperty("properies") List<PlayerProfileProperty> properties) {
            super(id, name);
            this.properties = properties;
        }

        public Optional<TexturesProperty> textures() {
            return properties.stream().filter(playerProfileProperty -> playerProfileProperty.name.equals("textures"))
                    .map(playerProfileProperty -> playerProfileProperty.value)
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

    public static final class PlayerProfileProperty {
        @JsonProperty("name")
        public final String name;
        @JsonProperty("value")
        public final String value;

        public PlayerProfileProperty(@JsonProperty("name") String name,
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
        @JsonProperty("agent")
        public final Agent agent;
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("selectedProfile")
        public final String selectedProfile;
        @JsonProperty("serverId")
        public final String serverId;

        @JsonCreator
        public JoinRequest(@JsonProperty("agent") Agent agent,
                           @JsonProperty("accessToken") String accessToken,
                           @JsonProperty("selectedProfile") String selectedProfile,
                           @JsonProperty("serverId") String serverId) {
            this.agent = agent;
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

    public static class JoinServerResponse {
        @JsonProperty("serverId")
        public final String serverId;

        public JoinServerResponse(@JsonProperty("serverId") String serverId) {
            this.serverId = serverId;
        }
    }

    public static final class AuthenticateRequest {
        @JsonProperty("agent")
        public final Agent agent;
        @JsonProperty("username")
        public final String username;
        @JsonProperty("password")
        public final String password;
        @JsonProperty("clientToken")
        public final UUID clientToken;
        @JsonProperty("requestUser")
        public final boolean requestUser;

        public AuthenticateRequest(@JsonProperty("agent") Agent agent,
                                   @JsonProperty("username") String username,
                                   @JsonProperty("password") String password,
                                   @JsonProperty("clientToken") UUID clientToken,
                                   @JsonProperty("requestUser") boolean requestUser) {
            this.agent = agent;
            this.username = username;
            this.password = password;
            this.clientToken = clientToken;
            this.requestUser = requestUser;
        }
    }

    public static final class AuthenticateResponse {
        @JsonProperty("clientToken")
        public final String clientToken;
        @JsonProperty("accessToken")
        public final String accessToken;
        @JsonProperty("selectedProfile")
        public final PlayerProfileInfo selectedProfile;
        @JsonProperty("user")
        public final PlayerPlayerProfile user;
        @JsonProperty("availableProfiles")
        public final List<PlayerProfileInfo> availableProfiles;

        public AuthenticateResponse(@JsonProperty("clientToken") String clientToken,
                                    @JsonProperty("accessToken") String accessToken,
                                    @JsonProperty("selectedProfile") PlayerProfileInfo selectedProfile,
                                    @JsonProperty("user") PlayerPlayerProfile user,
                                    @JsonProperty("availableProfiles") List<PlayerProfileInfo> availableProfiles) {
            this.clientToken = clientToken;
            this.accessToken = accessToken;
            this.selectedProfile = selectedProfile;
            this.user = user;
            this.availableProfiles = availableProfiles;
        }
    }

    public static class Agent {

        public static final Agent MINECRAFT = new Agent("Minecraft", 1);

        @JsonProperty("name")
        public final String name;
        @JsonProperty("version")
        public final Integer version;

        public Agent(@JsonProperty("name") String name,
                     @JsonProperty("version") Integer version) {
            this.name = name;
            this.version = version;
        }
    }

    public static String serverPublicKey() {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println("-----BEGIN PUBLIC KEY-----");
        printWriter.println(BaseEncoding.base32().encode(SERVER_KEY_PAIR.get().getPublic().getEncoded()));
        printWriter.println("-----END PUBLIC KEY-----");
        return writer.toString();
    }

    public static String toProfileId(UUID id) {
        return id.toString().replace("-", "");
    }

    private static KeyPair createKeyPair() {
        KeyPair kp;
        try {
            KeyPairGenerator keyPairGene = KeyPairGenerator.getInstance("RSA");
            keyPairGene.initialize(1024);
            kp = keyPairGene.genKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("problem generating the key", e);
        }
        return kp;
    }
}
