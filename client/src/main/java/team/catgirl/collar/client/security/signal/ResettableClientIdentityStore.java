package team.catgirl.collar.client.security.signal;

import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.identity.CreateTrustRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.Identity;

import java.io.IOException;
import java.util.UUID;
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
    public boolean isTrustedIdentity(Identity identity) {
        return currentIdentityStore.isTrustedIdentity(identity);
    }

    @Override
    public void trustIdentity(Identity owner, byte[] preKeyBundle) {
        currentIdentityStore.trustIdentity(owner, preKeyBundle);
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
    public int getDeviceId() {
        return currentIdentityStore.getDeviceId();
    }

    @Override
    public SendPreKeysRequest createSendPreKeysRequest(DeviceRegisteredResponse response) {
        return currentIdentityStore.createSendPreKeysRequest(response);
    }

    @Override
    public CreateTrustRequest createSendPreKeysRequest(ClientIdentity identity, long id) {
        return currentIdentityStore.createSendPreKeysRequest(identity, id);
    }

    @Override
    public JoinGroupRequest createJoinGroupRequest(ClientIdentity identity, UUID groupId) {
        return currentIdentityStore.createJoinGroupRequest(identity, groupId);
    }

    @Override
    public AcknowledgedGroupJoinedRequest processJoinGroupResponse(JoinGroupResponse resp) {
        return currentIdentityStore.processJoinGroupResponse(resp);
    }

    @Override
    public void processAcknowledgedGroupJoinedResponse(AcknowledgedGroupJoinedResponse response) {
        currentIdentityStore.processAcknowledgedGroupJoinedResponse(response);
    }

    @Override
    public void processLeaveGroupResponse(LeaveGroupResponse response) {
        currentIdentityStore.processLeaveGroupResponse(response);
    }

    @Override
    public void clearAllGroupSessions() {
        currentIdentityStore.clearAllGroupSessions();
    }

    @Override
    public void reset() throws IOException {
        currentIdentityStore.delete();
        currentIdentityStore = supplier.get();
    }
}
