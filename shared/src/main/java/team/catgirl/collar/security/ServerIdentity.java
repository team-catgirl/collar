package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.KeyPair.PublicKey;

import java.util.Objects;

/**
 * Identifies the server
 */
public final class ServerIdentity {
    @JsonProperty("publicKey")
    public final PublicKey publicKey;
    public final int id;

    public ServerIdentity(@JsonProperty("publicKey") PublicKey publicKey, @JsonProperty("id") int id) {
        this.publicKey = publicKey;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerIdentity that = (ServerIdentity) o;
        return publicKey.equals(that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicKey);
    }
}
