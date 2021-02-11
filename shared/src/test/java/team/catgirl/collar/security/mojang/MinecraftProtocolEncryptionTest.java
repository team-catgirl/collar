package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author charlie353535
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MinecraftProtocolEncryptionTest {
    private static final Logger LOGGER = Logger.getLogger(MinecraftProtocolEncryptionTest.class.getName());
    private static final OkHttpClient client = new OkHttpClient();
    private static MinecraftSession sess;

    static {
        try {
            sess = loginTest("test","test");
            MinecraftProtocolEncryption.ROOT_DOMAIN = "mjolnir.testing.charlie35.xyz";
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final MediaType JSON
            = MediaType.get("application/json");

    @Test
    public void _1_registration() throws Exception {
        boolean ret = MinecraftProtocolEncryption.joinServer(sess);
        if (ret) {
            LOGGER.info("Register passed!");
        } else {
            throw new RuntimeException("Error, register test failed!");
        }
    }

    @Test
    public void _2_verification() throws Exception {
        boolean ret = MinecraftProtocolEncryption.verifyClient("test", sess.id);
        if (ret) {
            LOGGER.info("Verify passed!");
        } else {
            throw new RuntimeException("Error, verify test failed!");
        }
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
