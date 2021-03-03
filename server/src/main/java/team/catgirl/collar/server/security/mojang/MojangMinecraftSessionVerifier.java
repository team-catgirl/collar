package team.catgirl.collar.server.security.mojang;

import team.catgirl.collar.security.mojang.MinecraftSession;
import team.catgirl.collar.security.mojang.ServerAuthentication;

import java.util.logging.Logger;

/**
 * Verifies identifies against Mojang auth servers
 */
public class MojangMinecraftSessionVerifier implements MinecraftSessionVerifier {

    private static final Logger LOGGER = Logger.getLogger(MojangMinecraftSessionVerifier.class.getName());
    private static final String NAME = "mojang";

    public final String baseUrl;

    public MojangMinecraftSessionVerifier(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean verify(MinecraftSession session) {
        ServerAuthentication authentication = new ServerAuthentication(baseUrl);
        return authentication.verifyClient(session);
    }
}
