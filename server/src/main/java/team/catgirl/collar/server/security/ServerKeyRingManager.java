package team.catgirl.collar.server.security;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.Rfc4880KeySelectionStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bson.Document;
import team.catgirl.collar.security.keyring.KeyRingManager;
import team.catgirl.collar.security.keys.KeyPair.PrivateKey;
import team.catgirl.collar.security.keys.KeyPair.PublicKey;
import team.catgirl.collar.security.messages.MessageCrypter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;

public class ServerKeyRingManager implements KeyRingManager {

//    private final KeyringConfig config;
    private final MongoCollection<Document> publicKeys;
    private final MongoCollection<Document> privateKeys;

    public ServerKeyRingManager(MongoDatabase db) {
        this.publicKeys = db.getCollection("publicKeys");
        this.privateKeys = db.getCollection("publicKeys");
    }

    @Override
    public void publicAndPrivateKey(PublicKey publicKey, PrivateKey privateKey) throws IOException {

    }

    @Override
    public void addPublicKey(PublicKey publicKey) throws IOException {

    }

    @Override
    public MessageCrypter crypter() {
        return new MessageCrypter(null);
    }

//    private final class KeySelectionStrategy extends Rfc4880KeySelectionStrategy {
//        public KeySelectionStrategy(Instant dateOfTimestampVerification, boolean matchPartial, boolean ignoreCase) {
//            super(dateOfTimestampVerification, false, false);
//        }
//
//        @Override
//        protected Set<PGPPublicKeyRing> publicKeyRingsForUid(PURPOSE purpose, String uid, KeyringConfig keyringConfig) throws IOException, PGPException {
//            final Set<PGPPublicKeyRing> keyringsForUid = new HashSet<>();
//
//
//            StreamSupport.stream(publicKeys.find(eq("uuid", uid)).map(ServerKeyRingManager::mapFrom), false)
//            return
//        }
//    }

    private final class ServerKeyringConfig implements KeyringConfig {
        @Override
        public PGPPublicKeyRingCollection getPublicKeyRings() throws IOException, PGPException {
            throw new IllegalStateException();
        }

        @Override
        public PGPSecretKeyRingCollection getSecretKeyRings() throws IOException, PGPException {
            throw new IllegalStateException();
        }

        @Nullable
        @Override
        public char[] decryptionSecretKeyPassphraseForSecretKeyId(long keyId) {
            return new char[0];
        }

        @Override
        public KeyFingerPrintCalculator getKeyFingerPrintCalculator() {
            return new BcKeyFingerprintCalculator();
        }
    }

    private static PublicKey mapFrom(Document document) {
        return null;
    }
}
