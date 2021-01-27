package team.catgirl.collar.server.security;

import team.catgirl.collar.messages.ClientMessage;
import team.catgirl.collar.messages.ClientMessage.CreateIdentityRequest;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;

public interface ServerIdentityStore {
    /**
     * @return identity of the server
     */
    ServerIdentity getIdentity();

    /**
     * Creates a new identity for a client
     * @param identity to create
     * @param req to create
     */
    void createIdentity(PlayerIdentity identity, CreateIdentityRequest req);

    /**
     * Tests if the identity trusted
     * @param identity to test
     * @return trusted or not
     */
    boolean isTrustedIdentity(PlayerIdentity identity);
}
