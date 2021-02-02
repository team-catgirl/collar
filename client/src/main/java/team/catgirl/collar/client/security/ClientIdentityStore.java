package team.catgirl.collar.client.security;

import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.ServerIdentity;

import java.io.IOException;

public interface ClientIdentityStore {
    /**
     * @return the players identity
     */
    ClientIdentity currentIdentity();

    /**
     * Tests if the server identity is trusted
     * @param identity to test
     * @return trusted or not
     */
    boolean isTrustedIdentity(ServerIdentity identity);

    /**
     * Trust the server identity
     * @param resp with PreKeyBundle
     */
    void trustIdentity(SendPreKeysResponse resp);

    /**
     * @return creates a new {@link Cypher}
     */
    Cypher createCypher();

    /**
     * @param deviceId of the registered device
     */
    void setDeviceId(int deviceId);

    int getDeviceId();

    /**
     * @return new {@link SendPreKeysRequest}
     * @param response
     */
    SendPreKeysRequest createSendPreKeysRequest(DeviceRegisteredResponse response);

    /**
     * Resets the identity store and recreates it
     * @throws IOException when store could not be recreated
     */
    void reset() throws IOException;
}
