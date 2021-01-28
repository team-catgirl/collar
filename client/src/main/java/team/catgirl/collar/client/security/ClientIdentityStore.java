package team.catgirl.collar.client.security;

import team.catgirl.collar.messages.ServerMessage;
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
     */
    void trustIdentity(ServerIdentity identity, ServerMessage.CreateIdentityResponse resp);

    /**
     * @return creates a new {@link Cypher}
     */
    Cypher createCypher();
}
