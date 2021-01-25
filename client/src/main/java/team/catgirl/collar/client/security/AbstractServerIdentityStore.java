package team.catgirl.collar.client.security;

import team.catgirl.collar.security.ServerIdentity;

import java.util.Arrays;
import java.util.UUID;

public abstract class AbstractServerIdentityStore implements ServerIdentityStore {

    @Override
    public final boolean fingerprintMatch(ServerIdentity identity) {
        ServerIdentity storedIdentity = getIdentity(identity.server);
        return storedIdentity != null && storedIdentity.server.equals(identity.server) && Arrays.equals(storedIdentity.publicKey.fingerPrint, identity.publicKey.fingerPrint);
    }

    @Override
    public final boolean isIdentityKnown(ServerIdentity identity) {
        ServerIdentity serverIdentity = getIdentity(identity.server);
        return serverIdentity != null && serverIdentity.equals(identity);
    }
}
