package team.catgirl.collar.server.security.mojang;

import team.catgirl.collar.security.mojang.MinecraftSession;

/**
 * Offline {@link MinecraftSessionVerifier}
 */
public class NojangMinecraftSessionVerifier implements MinecraftSessionVerifier {

    public static final String NAME = "nojang";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean verify(MinecraftSession session) {
        return true;
    }
}
