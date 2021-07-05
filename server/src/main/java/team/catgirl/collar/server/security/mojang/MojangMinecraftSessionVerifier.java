package team.catgirl.collar.server.security.mojang;

import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.protocol.session.StartSessionRequest;
import team.catgirl.collar.security.mojang.Mojang;

/**
 * Verifies identifies against Mojang auth servers
 */
public class MojangMinecraftSessionVerifier implements MinecraftSessionVerifier {

    private static final String NAME = "mojang";

    private final HttpClient http;
    public final String baseUrl;

    public MojangMinecraftSessionVerifier(HttpClient http, String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean verify(StartSessionRequest request) {
        return new Mojang(http, baseUrl).hasJoined(request.session, request.serverId);
    }
}
