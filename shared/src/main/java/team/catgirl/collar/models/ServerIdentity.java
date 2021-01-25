package team.catgirl.collar.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.Principal;
import team.catgirl.collar.security.PublicKey;

import java.util.UUID;

/**
 * Identifies the server
 */
public final class ServerIdentity implements Principal {
    public final UUID server;
    @JsonProperty("publicKey")
    public final PublicKey publicKey;

    public ServerIdentity(UUID server, @JsonProperty("publicKey") PublicKey publicKey) {
        this.server = server;
        this.publicKey = publicKey;
    }

    @Override
    public String getName() {
        return server.toString();
    }
}
