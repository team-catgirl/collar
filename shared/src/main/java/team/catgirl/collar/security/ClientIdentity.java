package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.KeyPair.PublicKey;

import java.util.Objects;
import java.util.UUID;

public final class ClientIdentity implements Identity {

    @JsonProperty("owner")
    public final UUID owner;
    @JsonProperty("publicKey")
    public final PublicKey publicKey;
    @JsonProperty("deviceId")
    public final Integer deviceId;

    public ClientIdentity(@JsonProperty("owner") UUID owner, @JsonProperty("publicKey") PublicKey publicKey, @JsonProperty("deviceId") Integer deviceId) {
        this.owner = owner;
        this.publicKey = publicKey;
        this.deviceId = deviceId;
    }

    @Override
    public UUID id() {
        return owner;
    }

    @Override
    public Integer deviceId() {
        return deviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientIdentity that = (ClientIdentity) o;
        return owner.equals(that.owner) &&
                publicKey.equals(that.publicKey) &&
                deviceId.equals(that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, publicKey, deviceId);
    }
}
