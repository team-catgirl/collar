package team.catgirl.collar.server.security;

import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.keyring.KeyRingManager;

import java.io.IOException;

public interface ServerIdentityProvider {
    ServerIdentity getIdentity(KeyRingManager keyRingManager) throws IOException;
}
