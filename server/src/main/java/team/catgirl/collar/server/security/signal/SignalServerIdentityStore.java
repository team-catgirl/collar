package team.catgirl.collar.server.security.signal;

import com.google.common.base.Suppliers;
import com.mongodb.client.MongoDatabase;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import team.catgirl.collar.messages.ClientMessage.CreateIdentityRequest;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.server.security.ServerIdentityStore;

import java.io.IOException;
import java.util.function.Supplier;

public class SignalServerIdentityStore implements ServerIdentityStore {

    private final SignalProtocolStore store;
    private final Supplier<ServerIdentity> serverIdentitySupplier;

    public SignalServerIdentityStore(MongoDatabase db) {
        this.store = ServerSignalProtocolStore.from(db);
        this.serverIdentitySupplier = Suppliers.memoize(() -> {
            IdentityKeyPair identityKeyPair = this.store.getIdentityKeyPair();
            IdentityKey publicKey = identityKeyPair.getPublicKey();
            int serverId = store.getLocalRegistrationId();
            return new ServerIdentity(new KeyPair.PublicKey(publicKey.getFingerprint(), publicKey.serialize()), serverId);
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
        return new SignalProtocolAddress(identity.player.toString(), identity.sessionId);
    }
}
