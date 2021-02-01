package team.catgirl.collar.server.security.signal;

import com.google.common.base.Suppliers;
import com.mongodb.client.MongoDatabase;
import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.state.PreKeyBundle;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.signal.PreKeys;
import team.catgirl.collar.security.signal.SignalCypher;
import team.catgirl.collar.server.security.ServerIdentityStore;

import java.io.IOException;
import java.util.UUID;
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

    public boolean isTrustedIdentity(ClientIdentity clientIdentity) {
        return store.isTrustedIdentity(signalProtocolAddressFrom(clientIdentity), identityKeyFrom(clientIdentity));
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

    @Override
    public UUID findIdentity(ClientIdentity identity, int deviceId) {
        String name = store.identityKeyStore.findNameBy(identityKeyFrom(identity), deviceId);
        return name == null ? null : UUID.fromString(name);
    }

    private static IdentityKey identityKeyFrom(ClientIdentity clientIdentity) {
        IdentityKey identityKey;
        try {
            identityKey = new IdentityKey(clientIdentity.publicKey.key, 0);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("bad key");
        }
        return identityKey;
    }

    private static SignalProtocolAddress signalProtocolAddressFrom(ClientIdentity identity) {
        return new SignalProtocolAddress(identity.id().toString(), 1);
    }
}
