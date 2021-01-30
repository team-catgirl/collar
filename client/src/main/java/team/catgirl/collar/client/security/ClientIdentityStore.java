package team.catgirl.collar.client.security;

import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;

public interface ClientIdentityStore {
    /**
     * @return the players identity
     */
    PlayerIdentity currentIdentity();

    /**
     * Tests if the server identity is trusted
     * @param identity to test
     * @return trusted or not
     */
    boolean isTrustedIdentity(ServerIdentity identity);

    /**
     * Trust the server identity
     * @param identity to trust
     * @param resp
     */
    void trustIdentity(ServerIdentity identity, SendPreKeysResponse resp);

    /**
     * @return creates a new {@link Cypher}
     */
    Cypher createCypher();

    void setDeviceId(int deviceId);

    SendPreKeysRequest createSendPreKeysRequest();
}
