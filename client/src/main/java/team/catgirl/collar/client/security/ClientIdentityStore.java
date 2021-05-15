package team.catgirl.collar.client.security;

import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.identity.CreateTrustRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.security.cipher.Cipher;

import java.io.IOException;
import java.util.UUID;

public interface ClientIdentityStore {
    /**
     * This token is stored by the server on first register and is used to check
     * if the private identity of the client has changed. If it has changed, we
     * the user has willingly generated a new token and forfeits any privately
     * encrypted state on the server
     * @return the players private identity token
     */
    byte[] privateIdentityToken();

    /**
     * @return the players identity
     */
    ClientIdentity currentIdentity();

    /**
     * Tests if the server identity is trusted
     * @param identity to test
     * @return trusted or not
     */
    boolean isTrustedIdentity(Identity identity);

    /**
     * Trusts the identity
     * @param owner identity
     * @param preKeyBundle belonging to the owner
     */
    void trustIdentity(Identity owner, byte[] preKeyBundle);

    /**
     * @return creates a new {@link Cipher}
     */
    Cipher createCypher();

    /**
     * @param response of the registered device
     */
    void processDeviceRegisteredResponse(DeviceRegisteredResponse response);

    /**
     * @return device id of this client
     */
    int getDeviceId();

    /**
     * @param response of the device registration
     * @return SendPreKeyRequest to send to the server
     */
    SendPreKeysRequest createSendPreKeysRequest(DeviceRegisteredResponse response);

    /**
     * @param identity joining group
     * @param groupId of the group being joined
     * @return join request
     */
    JoinGroupRequest createJoinGroupRequest(ClientIdentity identity, UUID groupId);

    /**
     * @param identity client identity to exchange keys with
     * @param id unique for this request
     * @return SendPreKeyRequest to send to the provided client identity
     */
    CreateTrustRequest createSendPreKeysRequest(ClientIdentity identity, long id);

    /**
     * Used to distribute keys back to the client who joined
     * @param resp of the join
     * @return acknowledged group joined request
     */
    AcknowledgedGroupJoinedRequest processJoinGroupResponse(JoinGroupResponse resp);

    /**
     * @param response to process
     */
    void processAcknowledgedGroupJoinedResponse(AcknowledgedGroupJoinedResponse response);

    /**
     * @param response to process
     */
    void processLeaveGroupResponse(LeaveGroupResponse response);

    /**
     * Clears all the current group sessions
     */
    void clearAllGroupSessions();

    /**
     * Resets the identity store and recreates it
     * @throws IOException when store could not be recreated
     */
    void reset() throws IOException;

    /**
     * Save state
     */
    void save() throws IOException;
}
