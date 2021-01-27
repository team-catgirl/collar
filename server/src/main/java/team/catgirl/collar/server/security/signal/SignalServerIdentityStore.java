package team.catgirl.collar.server.security.signal;

import com.google.common.base.Suppliers;
import com.mongodb.client.MongoDatabase;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.Medium;
import team.catgirl.collar.messages.ClientMessage.CreateIdentityRequest;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.signal.PreKeyBundles;
import team.catgirl.collar.security.signal.SignalCypher;
import team.catgirl.collar.server.security.ServerIdentityStore;

import java.io.IOException;
import java.util.Random;
import java.util.function.Supplier;

public class SignalServerIdentityStore implements ServerIdentityStore {

    private final ServerSignalProtocolStore store;
    private final Supplier<ServerIdentity> serverIdentitySupplier;

    public SignalServerIdentityStore(MongoDatabase db) {
        this.store = ServerSignalProtocolStore.from(db);
        this.serverIdentitySupplier = Suppliers.memoize(() -> {
            // TODO: signal client identity store uses the same code - make this common
            IdentityKeyPair identityKeyPair = this.store.getIdentityKeyPair();
            IdentityKey publicKey = identityKeyPair.getPublicKey();
            int registrationId = store.getLocalRegistrationId();
            ECKeyPair signedPreKey = Curve.generateKeyPair();
            // TODO: use secure random
            int signedPreKeyId = new Random().nextInt(Medium.MAX_VALUE);
            ECKeyPair unsignedPreKey = Curve.generateKeyPair();
            int unsighnedPreKeyId = new Random().nextInt(Medium.MAX_VALUE);
            byte[] signature;
            try {
                signature = Curve.calculateSignature(store.getIdentityKeyPair().getPrivateKey(), signedPreKey.getPublicKey().serialize());
            } catch (InvalidKeyException e) {
                throw new IllegalStateException("invalid key");
            }
            PreKeyBundle preKeyBundle = new PreKeyBundle(
                registrationId,
                1,
                unsighnedPreKeyId,
                unsignedPreKey.getPublicKey(),
                signedPreKeyId,
                signedPreKey.getPublicKey(),
                signature,
                store.getIdentityKeyPair().getPublicKey());

            store.storeSignedPreKey(signedPreKeyId, new SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), signedPreKey, signature));
            store.storePreKey(unsighnedPreKeyId, new PreKeyRecord(unsighnedPreKeyId, unsignedPreKey));

            byte[] bytes;
            try {
                bytes = PreKeyBundles.serialize(preKeyBundle);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return new ServerIdentity(
                    new KeyPair.PublicKey(publicKey.getFingerprint(), publicKey.serialize()),
                    store.identityKeyStore.getServerId(),
                    bytes
            );
        });
    }

    @Override
    public ServerIdentity getIdentity() {
        return serverIdentitySupplier.get();
    }

    @Override
    public void createIdentity(PlayerIdentity identity, CreateIdentityRequest req) {
        SignedPreKeyRecord record;
        try {
            record = new SignedPreKeyRecord(req.signedPreKey);
        } catch (IOException e) {
            throw new IllegalStateException("could not deserialized signed prekey");
        }
        req.preKeys.stream().map(serialized -> {
            try {
                return new PreKeyRecord(serialized);
            } catch (IOException e) {
                throw new IllegalStateException("could not deserialize prekey");
            }
        }).forEach(preKeyRecord -> store.storePreKey(preKeyRecord.getId(), preKeyRecord));
        store.storeSignedPreKey(record.getId(), record);
        store.saveIdentity(signalProtocolAddressFrom(identity), identityKeyFrom(identity));
    }

    public boolean isTrustedIdentity(PlayerIdentity playerIdentity) {
        return store.isTrustedIdentity(signalProtocolAddressFrom(playerIdentity), identityKeyFrom(playerIdentity));
    }

    @Override
    public Cypher createCypher() {
        return new SignalCypher(store);
    }

    private static IdentityKey identityKeyFrom(PlayerIdentity playerIdentity) {
        IdentityKey identityKey;
        try {
            identityKey = new IdentityKey(playerIdentity.publicKey.key, 0);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("bad key");
        }
        return identityKey;
    }

    private static SignalProtocolAddress signalProtocolAddressFrom(PlayerIdentity identity) {
        return new SignalProtocolAddress(identity.player.toString(), identity.registrationId);
    }
}
