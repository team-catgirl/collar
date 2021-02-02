package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.KeyPair.PublicKey;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies the server
 */
public final class ServerIdentity implements Identity {
    @JsonProperty("publicKey")
    public final PublicKey publicKey;
    @JsonProperty("serverId")
    public final UUID serverId;

    public ServerIdentity(@JsonProperty("publicKey") PublicKey publicKey, @JsonProperty("serverId") UUID serverId) {
        this.publicKey = publicKey;
        this.serverId = serverId;
    }

    @Override
    public UUID id() {
        return serverId;
    }

    @Override
    public Integer deviceId() {
        return 1;
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
