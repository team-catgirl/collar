package team.catgirl.collar.security.keys;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.generation.type.length.RsaLength;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.UUID;

/**
 * Generates PGP key pairs
 */
public class KeyPairGenerator {

    /**
     * Generate {@link KeyPair} for player with UUID
     * @param player the players id
     * @return key for server
     * @throws KeyPairGeneratorException on failure
     */
    public static KeyPair generateKeyPair(UUID player) throws KeyPairGeneratorException {
        String userId = player.toString();
        try {
            return generate(userId);
        } catch (PGPException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | IOException e) {
            throw new KeyPairGeneratorException(e);
        }
    }

    /**
     * Generate {@link KeyPair} for player with UUID
     * @param server the servers id (e.g. api.collarmc.com)
     * @return keyPair for server
     * @throws KeyPairGeneratorException on failure
     */
    public static KeyPair generateServerKeyPair(String server) throws KeyPairGeneratorException {
        try {
            return generate(server);
        } catch (PGPException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | IOException e) {
            throw new KeyPairGeneratorException(e);
        }
    }

    private static KeyPair generate(String userId) throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException {
        KeyringConfig rsaKeyRing = BouncyGPG.createSimpleKeyring().simpleRsaKeyRing(userId, RsaLength.RSA_2048_BIT);
        PGPPublicKey publicKey = rsaKeyRing.getPublicKeyRings().getKeyRings().next().getPublicKey();
        PGPSecretKey privateKey = rsaKeyRing.getSecretKeyRings().getKeyRings().next().getSecretKey();
        return new KeyPair(new KeyPair.PublicKey(publicKey.getFingerprint(), publicKey.getEncoded()), new KeyPair.PrivateKey(privateKey.getEncoded()));
    }

    private KeyPairGenerator() {}
}
