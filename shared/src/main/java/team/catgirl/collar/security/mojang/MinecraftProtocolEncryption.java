package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author charlie353535
 */
public class MinecraftProtocolEncryption {
    private static final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    public static String ROOT_DOMAIN = "mojang.com";

    private static final Logger LOGGER = Logger.getLogger(MinecraftProtocolEncryption.class.getName());

    public static boolean joinServer(MinecraftSession session) {
        try {
            ServerAuthentication.JoinRequest joinReq = new ServerAuthentication.JoinRequest(session.accessToken, session.id);

            byte[] json = Utils.jsonMapper().writeValueAsBytes(joinReq);

            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .url("https://sessionserver."+ROOT_DOMAIN+"/session/minecraft/join")
                    .post(body)
                    .build();
            LOGGER.info("Registering..");
            try (Response response = client.newCall(request).execute()) {
                if (response.code()!=204) {
                    LOGGER.log(Level.SEVERE, "Couldn't register!\n"+response.body().string());
                }
                return response.code()==204;
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.log(Level.SEVERE, "Couldn't register",e);
                return false;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Couldn't register",e);
            return false;
        }
    }

    public static boolean verifyClient(String username, UUID id) {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl
                    .parse("https://sessionserver."+ROOT_DOMAIN+"/session/minecraft/hasJoined").newBuilder();
            urlBuilder.addQueryParameter("username",username);
            urlBuilder.addQueryParameter("serverId",ServerAuthentication.genServerIDHash());
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .build();

            LOGGER.info("Verifying "+username);
            try (Response response = client.newCall(request).execute()) {
                JsonNode resp = Utils.jsonMapper().readTree(response.body().string());
                if (!resp.has("id")) {
                    LOGGER.log(Level.SEVERE, "Couldn't verify "+username.toUpperCase());
                    return false;
                }
                return response.code()==200 && resp.get("id").asText().equals(id.toString().replace("-", ""));
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Couldn't verify "+username.toUpperCase(),e);
            return false;
        }
    }
}

