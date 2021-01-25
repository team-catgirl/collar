package team.catgirl.collar.client.security;

import team.catgirl.collar.security.ServerIdentity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryServerIdentityStore implements ServerIdentityStore {

    private final Map<UUID, ServerIdentity> identities = new HashMap<>();

    @Override
    public ServerIdentity getIdentity(UUID server) {
        return identities.get(server);
    }

    @Override
    public ServerIdentity saveIdentity(ServerIdentity identity) {
        return identities.put(identity.server, identity);
    }

    @Override
    public boolean fingerprintMatch(ServerIdentity identity) {
        ServerIdentity storedIdentity = identities.get(identity.server);
        return storedIdentity != null && storedIdentity.server.equals(identity.server) && Arrays.equals(storedIdentity.publicKey.fingerPrint, identity.publicKey.fingerPrint);
    }

    @Override
    public boolean isIdentityKnown(ServerIdentity identity) {
        ServerIdentity serverIdentity = identities.get(identity.server);
        return serverIdentity != null && serverIdentity.equals(identity);
    }
}
