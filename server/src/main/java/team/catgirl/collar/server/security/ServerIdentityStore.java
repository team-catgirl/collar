package team.catgirl.collar.server.security;

import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public interface ServerIdentityStore {
    /**
     * @return identity of the server
     */
    ServerIdentity getIdentity();

    /**
     * Creates a new identity for a client
     * @param req to create
     */
    void trustIdentity(SendPreKeysRequest req);

    /**
     * Tests if the identity trusted
     * @param identity to test
     * @return trusted or not
     */
    boolean isTrustedIdentity(PlayerIdentity identity);

    /**
     * @return creates a new {@link Cypher}
     */
    Cypher createCypher();

    SendPreKeysResponse createSendPreKeysResponse();

    UUID findIdentity(PlayerIdentity identity, int deviceId);
}
