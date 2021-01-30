package team.catgirl.collar.server.security.signal;

import com.google.common.base.Suppliers;
import com.mongodb.client.MongoDatabase;
import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.state.PreKeyBundle;
import team.catgirl.collar.messages.ClientMessage.CreateIdentityRequest;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.signal.PreKeys;
import team.catgirl.collar.security.signal.SignalCypher;
import team.catgirl.collar.server.security.ServerIdentityStore;

import java.io.IOException;
import java.util.function.Supplier;

public class SignalServerIdentityStore implements ServerIdentityStore {

    private final ServerSignalProtocolStore store;
    private final Supplier<ServerIdentity> serverIdentitySupplier;

    public SignalServerIdentityStore(MongoDatabase db) {
        this.store = ServerSignalProtocolStore.from(db);
        this.serverIdentitySupplier = Suppliers.memoize(() -> {
            IdentityKey publicKey = store.getIdentityKeyPair().getPublicKey();
            return new ServerIdentity(
                new KeyPair.PublicKey(publicKey.getFingerprint(), publicKey.serialize())
            );
        });
    }

    @Override
    public ServerIdentity getIdentity() {
        return serverIdentitySupplier.get();
    }

    @Override
    public void trustIdentity(PlayerIdentity identity, CreateIdentityRequest req) {
        PreKeyBundle bundle;
        try {
            bundle = PreKeys.preKeyBundleFromBytes(req.signedPreKeyBundle);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
        try {
            return PreKeys.preKeyBundleToBytes(PreKeys.generate(store, 1));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
        return new SignalProtocolAddress(identity.id().toString(), 1);
    }
}
