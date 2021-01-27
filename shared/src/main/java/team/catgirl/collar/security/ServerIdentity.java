package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.KeyPair.PublicKey;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies the server
 */
public final class ServerIdentity {
    @JsonProperty("publicKey")
    public final PublicKey publicKey;
    @JsonProperty("serverId")
    public final UUID serverId;
    @JsonProperty("preKeyBundle")
    public byte[] preKeyBundle;

    public ServerIdentity(@JsonProperty("publicKey") PublicKey publicKey, @JsonProperty("serverId") UUID serverId, @JsonProperty("preKeyBundle") byte[] preKeyBundle) {
        this.publicKey = publicKey;
        this.serverId = serverId;
        this.preKeyBundle = preKeyBundle;
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
