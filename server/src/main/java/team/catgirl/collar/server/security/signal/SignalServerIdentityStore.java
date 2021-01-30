package team.catgirl.collar.server.security.signal;

import com.google.common.base.Suppliers;
import com.mongodb.client.MongoDatabase;
import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.state.PreKeyBundle;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
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
    public void trustIdentity(SendPreKeysRequest req) {
        PreKeyBundle bundle;
        try {
            bundle = PreKeys.preKeyBundleFromBytes(req.preKeyBundle);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        SignalProtocolAddress address = signalProtocolAddressFrom(req.identity);
        store.saveIdentity(address, bundle.getIdentityKey());
        SessionBuilder sessionBuilder = new SessionBuilder(store, address);
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
    public SendPreKeysResponse createSendPreKeysResponse() {
        PreKeyBundle bundle = PreKeys.generate(store, 1);
        try {
            return new SendPreKeysResponse(getIdentity(), PreKeys.preKeyBundleToBytes(bundle));
        } catch (IOException e) {
            throw new IllegalStateException("could not generate PreKeyBundle");
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
