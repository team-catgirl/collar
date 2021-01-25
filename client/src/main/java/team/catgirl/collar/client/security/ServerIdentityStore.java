package team.catgirl.collar.client.security;

import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.keys.KeyPair;

import java.util.UUID;

public interface ServerIdentityStore {
    ServerIdentity getIdentity(UUID server);
    ServerIdentity saveIdentity(ServerIdentity identity);
    boolean fingerprintMatch(ServerIdentity identity);
    boolean isIdentityKnown(ServerIdentity identity);
}
