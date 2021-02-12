package team.catgirl.collar.server.security.mojang;

import net.chris54721.openmcauthenticator.OpenMCAuthenticator;
import net.chris54721.openmcauthenticator.exceptions.AuthenticationUnavailableException;
import net.chris54721.openmcauthenticator.exceptions.RequestException;
import net.chris54721.openmcauthenticator.responses.RefreshResponse;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verifies identifies against Mojang auth servers
 */
@Deprecated
//Kept just in case
public class MojangMinecraftSessionVerifier implements MinecraftSessionVerifier {

    private static final Logger LOGGER = Logger.getLogger(MojangMinecraftSessionVerifier.class.getName());
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
