package team.catgirl.collar.client.security;

import team.catgirl.collar.security.ServerIdentity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryServerIdentityStore extends AbstractServerIdentityStore {

    private final Map<UUID, ServerIdentity> identities = new HashMap<>();

    @Override
    public ServerIdentity getIdentity(UUID server) {
        return identities.get(server);
    }

    @Override
    public ServerIdentity saveIdentity(ServerIdentity identity) {
        return identities.put(identity.server, identity);
    }
}
