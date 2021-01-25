package team.catgirl.collar.client.security;

import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.security.KeyPairGenerator;
import team.catgirl.collar.security.KeyPairGeneratorException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An in memory {@link PlayerIdentityStore}
 * <b>NOT FOR PRODUCTION USE</b>
 */
public class MemoryPlayerIdentityStore implements PlayerIdentityStore {

    private final Map<UUID, KeyPair> keyPairs = new HashMap<>();

    @Override
    public KeyPair keyPair(UUID player) throws IOException {
        return keyPairs.get(player);
    }

    @Override
    public KeyPair createKeyPair(UUID player) throws IOException, KeyPairGeneratorException {
        KeyPair keyPair = KeyPairGenerator.generateKeyPair(player);
        keyPairs.put(player, keyPair);
        return keyPair;
    }
}
