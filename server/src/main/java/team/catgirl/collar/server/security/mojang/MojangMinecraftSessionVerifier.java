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
public class MojangMinecraftSessionVerifier implements MinecraftSessionVerifier {

    private static final Logger LOGGER = Logger.getLogger(MojangMinecraftSessionVerifier.class.getName());
    private static final String NAME = "mojang";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean verify(MinecraftSession session) {
        if (session.accessToken == null || session.clientToken == null) {
            return false;
        }
        try {
            RefreshResponse resp = OpenMCAuthenticator.refresh(session.accessToken, session.clientToken);
            return resp.getSelectedProfile().getUUID().equals(session.id);
        } catch (RequestException | AuthenticationUnavailableException e) {
            LOGGER.log(Level.SEVERE, "Could not authenticate against Mojang", e);
            return false;
        }
    }
}
