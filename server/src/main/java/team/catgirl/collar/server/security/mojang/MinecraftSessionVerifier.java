package team.catgirl.collar.server.security.mojang;

import team.catgirl.collar.security.mojang.MinecraftSession;

/**
 * Verification of reported Minecraft session identity
 */
public interface MinecraftSessionVerifier {
    /**
     * Tests that the provided session is valid
     * @param session to test
     * @return valid
     */
    boolean verify(MinecraftSession session);
}
