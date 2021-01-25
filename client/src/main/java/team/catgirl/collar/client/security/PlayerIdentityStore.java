package team.catgirl.collar.client.security;

import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.keys.KeyPair;
import team.catgirl.collar.security.keys.KeyPairGeneratorException;

import java.io.IOException;
import java.util.UUID;

public interface PlayerIdentityStore {
    /**
     * Gets the current {@link KeyPair}
     * @param player id of player
     * @return null keypair or null if no key pair exists
     * @throws IOException on read error
     */
    KeyPair keyPair(UUID player) throws IOException;

    /**
     * Create a new {@link KeyPair} for the specified player
     * @param player
     * @return keypair for the player
     * @throws KeyPairGeneratorException on generation error
     * @throws IOException on create error
     */
    KeyPair createKeyPair(UUID player) throws KeyPairGeneratorException, IOException;

    /**
     * Create a new identity for the specified player
     * @param player id
     * @return a new identity object
     * @throws IOException on error
     * @throws KeyPairGeneratorException on generation error
     */
    default PlayerIdentity createIdentity(UUID player) throws IOException, KeyPairGeneratorException {
        KeyPair keyPair = keyPair(player);
        if (keyPair == null) {
            keyPair = createKeyPair(player);
        }
        return new PlayerIdentity(player, keyPair.publicKey);
    }
}
