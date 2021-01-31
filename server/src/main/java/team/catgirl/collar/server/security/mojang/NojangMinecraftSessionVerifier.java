package team.catgirl.collar.server.security.mojang;

import team.catgirl.collar.security.mojang.MinecraftSession;

/**
 * Offline {@link MinecraftSessionVerifier}
 */
public class NojangMinecraftSessionVerifier implements MinecraftSessionVerifier {
    @Override
    public boolean verify(MinecraftSession session) {
        return true;
    }
}
