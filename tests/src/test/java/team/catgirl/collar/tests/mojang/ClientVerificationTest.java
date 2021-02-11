package team.catgirl.collar.tests.mojang;

import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.security.mojang.AuthenticationJSON;
import team.catgirl.collar.security.mojang.MinecraftProtocolEncryption;
import team.catgirl.collar.security.mojang.MinecraftSession;
import team.catgirl.collar.server.Services;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.tests.junit.CollarClientRule;
import team.catgirl.collar.tests.junit.CollarServerRule;
import team.catgirl.collar.tests.junit.CollarTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class ClientVerificationTest {
    private static final Logger LOGGER = Logger.getLogger(ClientVerificationTest.class.getName());
    private static final OkHttpClient client = new OkHttpClient();
    private static MinecraftSession sess;

    public static final MediaType JSON
            = MediaType.get("application/json");

    private final AtomicReference<Services> services = new AtomicReference<>();
    private final AtomicReference<Profile> testProfile = new AtomicReference<>();
    private final AtomicInteger devicesConfirmed = new AtomicInteger(0);

    private final File tempDir = Files.createTempDir();

    static UUID testPlayerId;

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        this.services.set(services);
        testProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                "alice@example.com",
                "alice",
                "Alice UwU"
        )).profile);
    }).useSecureConfiguration(true);

    @Rule
    public CollarClientRule testClientRule;

    @Test
    public void initialRegistrationAndLoginDoesNotExplode() throws Exception {
        MinecraftProtocolEncryption.ROOT_DOMAIN = "mjolnir.testing.charlie35.xyz";
        sess = loginTest("test","test");
        testPlayerId = sess.id;

        waitForCondition("logged in", () -> sess!=null);

        waitForCondition("server started", () -> serverRule.isServerStarted());

        testClientRule = new CollarClientRule(testPlayerId, new CollarConfiguration.Builder()
                .withListener(new CollarTest.ApprovingListener(testProfile, services, devicesConfirmed))
                .withHomeDirectory(tempDir),
                MinecraftSession.from(sess.id, sess.username, sess.accessToken, sess.server)
        );

        testClientRule.collar = Collar.create(testClientRule.builder.build());
        testClientRule.thread.start();

        //waitForCondition("collar != null", () -> testClientRule.collar!=null);

        waitForCondition("device registered", () -> devicesConfirmed.get() == 1);
        waitForCondition("client connected", () -> testClientRule.collar.getState() == Collar.State.CONNECTED);

        // Disconnect alice
        testClientRule.collar.disconnect();

        waitForCondition("client disconnected", () -> testClientRule.collar.getState() == Collar.State.DISCONNECTED);

        devicesConfirmed.set(0);

        testClientRule.collar.connect();

        waitForCondition("client connected", () -> testClientRule.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("device registered", () -> devicesConfirmed.get() == 0);
    }

    private static MinecraftSession loginTest(String user, String pass) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        byte[] req = Utils.jsonMapper().writeValueAsBytes(new AuthenticationJSON("test","test"));

        LOGGER.info("Logging in to testing session server as test:test");

        RequestBody body = RequestBody.create(JSON, req);
        Request request = new Request.Builder()
                .addHeader("Content-Type", "application/json")
                .url("http://authserver.mjolnir.testing.charlie35.xyz/authenticate")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String body_ = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(body_);
            if (jsonNode.has("error")) {
                LOGGER.severe("Cannot log in! "+jsonNode.get("errorMessage").asText()+"\n"+body_);
                return null;
            }
            JsonNode selectedProfile = jsonNode.get("selectedProfile");
            LOGGER.info("Token: "+ jsonNode.get("accessToken").asText()+"\nUUID: "+insertDashUUID(selectedProfile.get("id").asText()));
            MinecraftSession session = new MinecraftSession(
                    UUID.fromString(insertDashUUID(selectedProfile.get("id").asText())),
                    selectedProfile.get("name").asText(),
                    jsonNode.get("accessToken").asText(),
                    "testing.charlie35.xyz");
            LOGGER.info("Logged in! "+session.toString());
            return session;
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Couldn't log in",e);
            return null;
        }
    }

    public static String insertDashUUID(String uuid) {
        StringBuilder sb = new StringBuilder(uuid);
        sb.insert(8, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(13, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(18, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(23, "-");

        return sb.toString();
    }
}
