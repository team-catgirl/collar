package team.catgirl.collar.server.security;

import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.security.KeyPairGenerator;
import team.catgirl.collar.security.KeyPairGeneratorException;

import java.util.UUID;

public class MemoryServerKeyPairProvider implements ServerKeyPairProvider {

    private final KeyPair keyPair;

    public MemoryServerKeyPairProvider() throws KeyPairGeneratorException {
        keyPair = KeyPairGenerator.generateServerKeyPair(UUID.randomUUID().toString());
    }

    @Override
    public KeyPair get() {
        return keyPair;
    }
}
