package team.catgirl.collar.server.security.mojang;

import net.chris54721.openmcauthenticator.OpenMCAuthenticator;
import net.chris54721.openmcauthenticator.exceptions.AuthenticationUnavailableException;
import net.chris54721.openmcauthenticator.exceptions.RequestException;
import net.chris54721.openmcauthenticator.responses.RefreshResponse;
import team.catgirl.collar.security.mojang.MinecraftSession;

/**
 * Verifies identifies against Mojang auth servers
 */
public class MojangMinecraftSessionVerifier implements MinecraftSessionVerifier {
    @Override
    public boolean verify(MinecraftSession session) {
        try {
            RefreshResponse resp = OpenMCAuthenticator.refresh(session.accessToken, session.clientToken);
            return resp.getSelectedProfile().getUUID().equals(session.id);
        } catch (RequestException | AuthenticationUnavailableException e) {
            return false;
        }
    }
}
