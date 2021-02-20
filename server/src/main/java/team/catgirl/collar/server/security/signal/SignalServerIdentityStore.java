package team.catgirl.collar.server.security.signal;

import com.google.common.base.Suppliers;
import com.mongodb.client.MongoDatabase;
import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.SessionRecord;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.security.*;
import team.catgirl.collar.security.signal.PreKeys;
import team.catgirl.collar.security.signal.SignalCypher;
import team.catgirl.collar.server.security.ServerIdentityStore;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SignalServerIdentityStore implements ServerIdentityStore {

    private static final Logger LOGGER = Logger.getLogger(SignalServerIdentityStore.class.getName());

    private final ServerSignalProtocolStore store;
    private final Supplier<ServerIdentity> serverIdentitySupplier;

    public SignalServerIdentityStore(MongoDatabase db) {
        this.store = ServerSignalProtocolStore.from(db);
        this.serverIdentitySupplier = Suppliers.memoize(() -> {
            IdentityKey publicKey = store.getIdentityKeyPair().getPublicKey();
            return new ServerIdentity(
                new PublicKey(publicKey.serialize()),
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
        if (isTrustedIdentity(req.identity)) {
            throw new IllegalStateException(req.identity + " is already trusted");
        }
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
        SessionRecord sessionRecord = store.loadSession(address);
        sessionRecord.getSessionState().clearUnacknowledgedPreKeyMessage();
        LOGGER.log(Level.INFO, "Trust established with " + address);
    }

    public boolean isTrustedIdentity(ClientIdentity clientIdentity) {
        return store.isTrustedIdentity(signalProtocolAddressFrom(clientIdentity), identityKeyFrom(clientIdentity), null);
    }

    @Override
    public Cypher createCypher() {
        return new SignalCypher(null, store, null);
    }

    @Override
    public SendPreKeysResponse createSendPreKeysResponse() {
        PreKeyBundle bundle = PreKeys.generate(getIdentity(), store);
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
        return new SignalProtocolAddress(identity.id().toString(), identity.deviceId);
    }
}
