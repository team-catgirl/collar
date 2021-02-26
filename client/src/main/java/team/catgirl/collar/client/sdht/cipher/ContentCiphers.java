package team.catgirl.collar.client.sdht.cipher;

import team.catgirl.collar.sdht.Content;
import team.catgirl.collar.sdht.cipher.ContentCipher;
import team.catgirl.collar.security.ClientIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Holds all the available {@link ContentCipher}'s for SDHT name spaces
 */
public final class ContentCiphers implements ContentCipher {
    private final List<ContentCipher> ciphers = new ArrayList<>();

    /**
     * Register a {@link ContentCipher}
     * @param cipher to register
     */
    public void register(ContentCipher cipher) {
        ciphers.add(cipher);
    }

    @Override
    public byte[] crypt(ClientIdentity identity, UUID namespace, Content bytes) {
        ContentCipher cipher = ciphers.stream()
                .filter(recordCipher -> recordCipher.accepts(namespace))
                .findFirst().orElseThrow(() -> new IllegalStateException("cannot find cipher for namespace " + namespace));
        return cipher.crypt(identity, namespace, bytes);
    }

    @Override
    public Content decrypt(ClientIdentity identity, UUID namespace, byte[] bytes) {
        ContentCipher cipher = ciphers.stream()
                .filter(recordCipher -> recordCipher.accepts(namespace))
                .findFirst().orElseThrow(() -> new IllegalStateException("cannot find cipher for namespace " + namespace));
        return cipher.decrypt(identity, namespace, bytes);
    }

    @Override
    public boolean accepts(UUID namespace) {
        throw new IllegalStateException("not implemented");
    }
}
