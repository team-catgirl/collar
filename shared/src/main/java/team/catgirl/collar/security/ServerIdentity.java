package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    public final KeyPair.PrivateKey privateKey;

    @JsonCreator
    public ServerIdentity(
            @JsonProperty("server") UUID server,
            @JsonProperty("publicKey") KeyPair.PublicKey publicKey) {
        this.server = server;
        this.publicKey = publicKey;
        this.privateKey = null;
    }

    public ServerIdentity(UUID server, KeyPair.PublicKey publicKey, KeyPair.PrivateKey privateKey) {
        this.server = server;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
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
        return server.equals(that.server) && publicKey.equals(that.publicKey) && Objects.equals(privateKey, that.privateKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(server, publicKey, privateKey);
    }
}
