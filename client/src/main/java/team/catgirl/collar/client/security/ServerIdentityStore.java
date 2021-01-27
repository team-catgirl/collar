package team.catgirl.collar.client.security;

import team.catgirl.collar.security.KeyPair;

public interface ServerIdentityStore {
    KeyPair.PublicKey getIdentity(String server);
    KeyPair.PublicKey saveIdentity(String server);
}
