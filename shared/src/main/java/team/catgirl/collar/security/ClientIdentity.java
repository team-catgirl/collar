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

    public ClientIdentity(@JsonProperty("owner") UUID owner, @JsonProperty("publicKey") PublicKey publicKey) {
        this.owner = owner;
        this.publicKey = publicKey;
    }

    @Override
    public UUID id() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientIdentity clientIdentity = (ClientIdentity) o;
        return owner.equals(clientIdentity.owner) &&
                publicKey.equals(clientIdentity.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, publicKey);
    }
}
