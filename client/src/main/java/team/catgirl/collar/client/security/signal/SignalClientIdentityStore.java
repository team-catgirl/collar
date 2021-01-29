package team.catgirl.collar.client.security.signal;

import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.signal.PreKeys;
import team.catgirl.collar.security.signal.SignalCypher;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
        IdentityKeyPair identityKeyPair = this.store.getIdentityKeyPair();
        return new PlayerIdentity(player, new KeyPair.PublicKey(identityKeyPair.getPublicKey().getFingerprint(), identityKeyPair.getPublicKey().serialize()), state.registrationId);
    }

    @Override
    public boolean isTrustedIdentity(ServerIdentity identity) {
        return store.isTrustedIdentity(signalProtocolAddressFrom(identity), identityKeyFrom(identity));
    }

    @Override
    public void trustIdentity(ServerIdentity identity, ServerMessage.CreateIdentityResponse resp) {
        PreKeyBundle bundle;
        try {
            bundle = PreKeys.preKeyBundleFromBytes(resp.preKeyBundle);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        SessionBuilder sessionBuilder = new SessionBuilder(store, signalProtocolAddressFrom(identity));
        try {
            sessionBuilder.process(bundle);
        } catch (InvalidKeyException | UntrustedIdentityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Cypher createCypher() {
        return new SignalCypher(store);
    }

    private SignalProtocolAddress signalProtocolAddressFrom(ServerIdentity serverIdentity) {
        return new SignalProtocolAddress(serverIdentity.id().toString(), 1);
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

    public static ClientIdentityStore from(UUID player, HomeDirectory homeDirectory, Consumer<SignalProtocolStore> onInstall) throws IOException {
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
            onInstall.accept(store);
        }
        return new SignalClientIdentityStore(player, store, state);
    }

    private static void writeState(File file, State state) throws IOException {
        Utils.createObjectMapper().writeValue(file, state);
    }
}
