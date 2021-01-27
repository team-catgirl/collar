package team.catgirl.collar.client.security.signal;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.Medium;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.signal.PreKeyBundles;
import team.catgirl.collar.security.signal.SignalCypher;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class SignalClientIdentityStore implements ClientIdentityStore {

    private final UUID player;
    private final SignalProtocolStore store;
    private final State state;

    public SignalClientIdentityStore(UUID player, SignalProtocolStore store, State state) {
        this.player = player;
        this.store = store;
        this.state = state;
    }

    @Override
    public PlayerIdentity currentIdentity() {
        try {
            IdentityKeyPair identityKeyPair = this.store.getIdentityKeyPair();
            ECKeyPair signedPreKey = Curve.generateKeyPair();
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
            byte[] bytes = PreKeyBundles.serialize(preKeyBundle);
            return new PlayerIdentity(player, new KeyPair.PublicKey(identityKeyPair.getPublicKey().getFingerprint(), identityKeyPair.getPublicKey().serialize()), state.registrationId, bytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isTrustedIdentity(ServerIdentity identity) {
        return store.isTrustedIdentity(signalProtocolAddressFrom(identity), identityKeyFrom(identity));
    }

    @Override
    public void trustIdentity(ServerIdentity identity) {
        SignalProtocolAddress address = signalProtocolAddressFrom(identity);
        IdentityKey identityKey = identityKeyFrom(identity);
        if (!isTrustedIdentity(identity)) {
            store.saveIdentity(address, identityKey);
        }
    }

    @Override
    public Cypher createCypher() {
        return new SignalCypher(store);
    }

    private SignalProtocolAddress signalProtocolAddressFrom(ServerIdentity serverIdentity) {
        return new SignalProtocolAddress(serverIdentity.serverId.toString(), 1);
    }

    private static IdentityKey identityKeyFrom(ServerIdentity identity) {
        IdentityKey identityKey;
        try {
            identityKey = new IdentityKey(identity.publicKey.key, 0);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("bad key");
        }
        return identityKey;
    }

    private static final class State {
        public final byte[] identityKeyPair;
        public final Integer registrationId;

        public State(byte[] identityKeyPair, Integer registrationId) {
            this.identityKeyPair = identityKeyPair;
            this.registrationId = registrationId;
        }
    }

    public static ClientIdentityStore from(UUID player, HomeDirectory homeDirectory, BiConsumer<SignedPreKeyRecord, List<PreKeyRecord>> onInstall) throws IOException {
        SignalProtocolStore store = ClientSignalProtocolStore.from(homeDirectory);
        File file = new File(homeDirectory.security(), "identity.json");
        State state;
        if (file.exists()) {
            state = Utils.createObjectMapper().readValue(file, State.class);
        } else {
            // Generate the new identity, its prekeys, etc
            IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
            int registrationId  = KeyHelper.generateRegistrationId(false);
            List<PreKeyRecord> preKeys = KeyHelper.generatePreKeys(0, 500);
            SignedPreKeyRecord signedPreKey;
            try {
                signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 0);
            } catch (InvalidKeyException e) {
                throw new IllegalStateException("problem generating signed preKey", e);
            }
            preKeys.forEach(preKeyRecord -> store.storePreKey(preKeyRecord.getId(), preKeyRecord));
            store.storeSignedPreKey(signedPreKey.getId(), signedPreKey);
            state = new State(identityKeyPair.serialize(), registrationId);
            // Save the identity state
            writeState(file, state);
            // fire the on install consumer
            onInstall.accept(signedPreKey, preKeys);
        }
        return new SignalClientIdentityStore(player, store, state);
    }

    private static void writeState(File file, State state) throws IOException {
        Utils.createObjectMapper().writeValue(file, state);
    }
}
