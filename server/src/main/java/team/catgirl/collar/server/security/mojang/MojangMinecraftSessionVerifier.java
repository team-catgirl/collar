package team.catgirl.collar.server.security.mojang;

import okhttp3.OkHttpClient;
import team.catgirl.collar.security.mojang.MinecraftSession;
import team.catgirl.collar.security.mojang.ServerAuthentication;

import java.util.logging.Logger;

/**
 * Verifies identifies against Mojang auth servers
 */
public class MojangMinecraftSessionVerifier implements MinecraftSessionVerifier {

    private static final String NAME = "mojang";

    private final OkHttpClient http;
    public final String baseUrl;

    public MojangMinecraftSessionVerifier(OkHttpClient http, String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean verify(MinecraftSession session) {
        return new ServerAuthentication(http, baseUrl).verifyClient(session);
    }
}
