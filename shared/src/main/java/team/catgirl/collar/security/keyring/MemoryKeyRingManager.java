package team.catgirl.collar.security.keyring;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallback;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import team.catgirl.collar.security.keys.KeyPair.PrivateKey;
import team.catgirl.collar.security.keys.KeyPair.PublicKey;
import team.catgirl.collar.security.messages.MessageCrypter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

public class MemoryKeyRingManager implements KeyRingManager {

    private final KeyFingerPrintCalculator keyFingerPrintCalculator = new BcKeyFingerprintCalculator();

    private final InMemoryKeyring config;

    public MemoryKeyRingManager() {
        try {
            config = new InMemoryKeyring(keyId -> null);
        } catch (IOException | PGPException e) {
            throw new IllegalStateException(e);
        }
    }

    public MessageCrypter crypter() {
        return new MessageCrypter(config);
    }

    @Override
    public void publicAndPrivateKey(PublicKey publicKey, PrivateKey privateKey) throws IOException {
        try {
            config.addPublicKey(publicKey.bytes);
            config.addSecretKey(privateKey.bytes);
        } catch (PGPException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void addPublicKey(PublicKey publicKey) throws IOException {
        try {
            config.addPublicKey(publicKey.bytes);
        } catch (PGPException e) {
            throw new IOException(e);
        }
    }

    // Copied from name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring
    private final class InMemoryKeyring implements KeyringConfig {
        private final KeyringConfigCallback callback;
        private PGPPublicKeyRingCollection publicKeyRings;
        private PGPSecretKeyRingCollection secretKeyRings;

        InMemoryKeyring(KeyringConfigCallback callback) throws IOException, PGPException {
            Objects.requireNonNull(callback, "callback must not be null");
            this.callback = callback;
            this.publicKeyRings = new PGPPublicKeyRingCollection(new ArrayList<>());
            this.secretKeyRings = new PGPSecretKeyRingCollection(new ArrayList<>());
        }

        public void addPublicKey(byte[] encodedPublicKey) throws IOException, PGPException {
            Objects.requireNonNull(encodedPublicKey, "encodedPublicKey must not be null");
            ByteArrayInputStream raw = new ByteArrayInputStream(encodedPublicKey);
            try (InputStream decoded = PGPUtil.getDecoderStream(raw)) {
                try {
                    PGPPublicKeyRing pgpPub = new PGPPublicKeyRing(decoded, this.getKeyFingerPrintCalculator());
                    this.addPublicKeyRing(pgpPub);
                } catch (Throwable var8) {
                    if (decoded != null) {
                        try {
                            decoded.close();
                        } catch (Throwable ex) {
                            var8.addSuppressed(ex);
                        }
                    }
                    throw var8;
                }
                if (decoded != null) {
                    decoded.close();
                }
            } catch (Throwable var9) {
                try {
                    raw.close();
                } catch (Throwable var6) {
                    var9.addSuppressed(var6);
                }
                throw var9;
            }
            raw.close();
        }

        public void addSecretKey(byte[] encodedPrivateKey) throws IOException, PGPException {
            Objects.requireNonNull(encodedPrivateKey, "encodedPrivateKey must not be null");
            ByteArrayInputStream raw = new ByteArrayInputStream(encodedPrivateKey);

            try {
                InputStream decoded = PGPUtil.getDecoderStream(raw);

                try {
                    PGPSecretKeyRing pgpPRivate = new PGPSecretKeyRing(decoded, this.getKeyFingerPrintCalculator());
                    this.addSecretKeyRing(pgpPRivate);
                } catch (Throwable var8) {
                    if (decoded != null) {
                        try {
                            decoded.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }
                    }

                    throw var8;
                }

                if (decoded != null) {
                    decoded.close();
                }
            } catch (Throwable var9) {
                try {
                    raw.close();
                } catch (Throwable var6) {
                    var9.addSuppressed(var6);
                }

                throw var9;
            }

            raw.close();
        }

        public void addSecretKeyRing(PGPSecretKeyRing keyring) {
            Objects.requireNonNull(keyring, "keyring must not be null");
            this.secretKeyRings = PGPSecretKeyRingCollection.addSecretKeyRing(this.secretKeyRings, keyring);
        }

        public void addPublicKeyRing(PGPPublicKeyRing keyring) {
            Objects.requireNonNull(keyring, "keyring must not be null");
            this.publicKeyRings = PGPPublicKeyRingCollection.addPublicKeyRing(this.publicKeyRings, keyring);
        }

        @Nonnull
        public PGPPublicKeyRingCollection getPublicKeyRings() throws IOException, PGPException {
            return this.publicKeyRings;
        }

        @Nonnull
        public PGPSecretKeyRingCollection getSecretKeyRings() throws IOException, PGPException {
            return this.secretKeyRings;
        }

        @Nullable
        public char[] decryptionSecretKeyPassphraseForSecretKeyId(long keyID) {
            return this.callback.decryptionSecretKeyPassphraseForSecretKeyId(keyID);
        }

        public KeyFingerPrintCalculator getKeyFingerPrintCalculator() {
            return keyFingerPrintCalculator;
        }
    }
}
