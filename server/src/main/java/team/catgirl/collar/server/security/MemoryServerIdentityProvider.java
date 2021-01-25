package team.catgirl.collar.server.security;

import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.keys.KeyPair;
import team.catgirl.collar.security.keys.KeyPairGenerator;
import team.catgirl.collar.security.keys.KeyPairGeneratorException;

import java.util.UUID;

public class MemoryServerIdentityProvider implements ServerIdentityProvider {

    private final UUID serverId;
    private final KeyPair keyPair;

    public MemoryServerIdentityProvider() throws KeyPairGeneratorException {
        this.serverId = UUID.randomUUID();
        keyPair = KeyPairGenerator.generateServerKeyPair(serverId.toString());
    }

    @Override
    public ServerIdentity getIdentity() {
        return new ServerIdentity(serverId, getIdentity().publicKey);
    }
}
