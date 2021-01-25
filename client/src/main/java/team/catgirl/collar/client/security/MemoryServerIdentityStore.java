package team.catgirl.collar.client.security;

import team.catgirl.collar.security.ServerIdentity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An in memory {@link MemoryServerIdentityStore}
 * <b>NOT FOR PRODUCTION USE</b>
 */
public final class MemoryServerIdentityStore extends AbstractServerIdentityStore {

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
