package team.catgirl.collar.server.security.signal;

import com.google.common.base.Suppliers;
import com.mongodb.client.MongoDatabase;
import org.whispersystems.libsignal.*;
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

            return new ServerIdentity(
                    new KeyPair.PublicKey(publicKey.getFingerprint(), publicKey.serialize()),
                    store.identityKeyStore.getServerId()
            );
        });
    }

    @Override
    public ServerIdentity getIdentity() {
        return serverIdentitySupplier.get();
    }

    @Override
    public void createIdentity(PlayerIdentity identity, CreateIdentityRequest req) {
        PreKeyBundle bundle;
        try {
            bundle = PreKeyBundles.deserialize(req.signedPreKeyBundle);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        store.saveIdentity(signalProtocolAddressFrom(identity), bundle.getIdentityKey());
        SessionBuilder sessionBuilder = new SessionBuilder(store, signalProtocolAddressFrom(identity));
        try {
            sessionBuilder.process(bundle);
        } catch (InvalidKeyException|UntrustedIdentityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isTrustedIdentity(PlayerIdentity playerIdentity) {
        return store.isTrustedIdentity(signalProtocolAddressFrom(playerIdentity), identityKeyFrom(playerIdentity));
    }

    @Override
    public Cypher createCypher() {
        return new SignalCypher(store);
    }

    @Override
    public byte[] generatePreKeyBundle() {
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
                store.getLocalRegistrationId(),
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
        return bytes;
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
        return new SignalProtocolAddress(identity.player.toString(), 1);
    }
}
