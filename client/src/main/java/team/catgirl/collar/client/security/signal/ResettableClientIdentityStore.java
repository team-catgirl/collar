package team.catgirl.collar.client.security.signal;

import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.ServerIdentity;

import java.io.IOException;
import java.util.function.Supplier;

public class ResettableClientIdentityStore implements ClientIdentityStore {
    private SignalClientIdentityStore currentIdentityStore;
    private final Supplier<SignalClientIdentityStore> supplier;

    public ResettableClientIdentityStore(Supplier<SignalClientIdentityStore> supplier) {
        this.currentIdentityStore = supplier.get();
        this.supplier = supplier;
    }

    @Override
    public ClientIdentity currentIdentity() {
        return currentIdentityStore.currentIdentity();
    }

    @Override
    public boolean isTrustedIdentity(ServerIdentity identity) {
        return currentIdentityStore.isTrustedIdentity(identity);
    }

    @Override
    public void trustIdentity(SendPreKeysResponse resp) {
        currentIdentityStore.trustIdentity(resp);
    }

    @Override
    public Cypher createCypher() {
        return currentIdentityStore.createCypher();
    }

    @Override
    public void setDeviceId(int deviceId) {
        currentIdentityStore.setDeviceId(deviceId);
    }

    @Override
    public SendPreKeysRequest createSendPreKeysRequest() {
        return currentIdentityStore.createSendPreKeysRequest();
    }

    @Override
    public void reset() throws IOException {
        currentIdentityStore.delete();
        currentIdentityStore = supplier.get();
    }
}
