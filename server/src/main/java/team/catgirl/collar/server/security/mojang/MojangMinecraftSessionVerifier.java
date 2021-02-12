package team.catgirl.collar.server.security.mojang;

import team.catgirl.collar.security.mojang.MinecraftSession;

/**
 * Verifies identifies against Mojang auth servers
 */
@Deprecated
public class MojangMinecraftSessionVerifier implements MinecraftSessionVerifier {
    private static final String NAME = "mojang";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean verify(MinecraftSession session) {
        return false;
    }
}
