package team.catgirl.collar.security.mojang;

import okhttp3.*;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MinecraftProtocolEncryption {
    private static final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private static final Logger LOGGER = Logger.getLogger(MinecraftProtocolEncryption.class.getName());

    public static boolean joinServer(MinecraftSession session, String serverID) {
        try {
            ServerAuthentication.JoinRequest joinReq = new ServerAuthentication.JoinRequest(session.accessToken,
                    session.id,
                    "");

            byte[] json = Utils.jsonMapper().writeValueAsBytes(joinReq);

            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url("https://sessionserver.mojang.com/session/minecraft/join")
                    .post(body)
                    .build();
            LOGGER.info("Registering with "+new String(json));
            try (Response response = client.newCall(request).execute()) {
                return response.code()==204;
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.log(Level.SEVERE, "Couldn't register",e);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Couldn't register",e);
            return false;
        }
    }
}

