package team.catgirl.collar.security.keyring;

import org.bouncycastle.openpgp.PGPException;
import team.catgirl.collar.security.keys.KeyPair.PrivateKey;
import team.catgirl.collar.security.keys.KeyPair.PublicKey;
import team.catgirl.collar.security.messages.MessageCrypter;

import java.io.IOException;

public interface KeyRingManager {
    void publicAndPrivateKey(PublicKey publicKey, PrivateKey privateKey) throws IOException;
    void addPublicKey(PublicKey publicKey) throws IOException;
    MessageCrypter crypter();
}
