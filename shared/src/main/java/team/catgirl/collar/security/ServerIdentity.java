package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.keys.KeyPair;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies the server
 */
public final class ServerIdentity implements Identity {
    @JsonProperty("server")
    public final UUID server;
    @JsonProperty("publicKey")
    public final KeyPair.PublicKey publicKey;

    public ServerIdentity(@JsonProperty("server") UUID server, @JsonProperty("publicKey") KeyPair.PublicKey publicKey) {
        this.server = server;
        this.publicKey = publicKey;
    }

    @Override
    public String getName() {
        return server.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerIdentity that = (ServerIdentity) o;
        return server.equals(that.server) &&
                publicKey.equals(that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(server, publicKey);
    }
}
