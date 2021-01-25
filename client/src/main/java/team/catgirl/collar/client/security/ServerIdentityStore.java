package team.catgirl.collar.client.security;

import team.catgirl.collar.security.PublicKey;

public interface ServerIdentityStore {
    PublicKey getIdentity(String server);
    PublicKey saveIdentity(String server);
}
