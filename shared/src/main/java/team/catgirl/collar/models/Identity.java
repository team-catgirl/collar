package team.catgirl.collar.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.Principal;
import team.catgirl.collar.security.PublicKey;

import java.util.Objects;
import java.util.UUID;

public final class Identity implements Principal {

    @JsonProperty("player")
    public final UUID player;
    @JsonProperty("publicKey")
    private final PublicKey publicKey;

    public Identity(@JsonProperty("player") UUID player, @JsonProperty("publicKey") PublicKey publicKey) {
        this.player = player;
        this.publicKey = publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        return player.equals(identity.player) &&
                publicKey.equals(identity.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, publicKey);
    }

    @Override
    public String getName() {
        return player.toString();
    }
}
