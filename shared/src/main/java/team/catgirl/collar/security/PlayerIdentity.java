package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.KeyPair.PublicKey;

import java.util.Objects;
import java.util.UUID;

public final class PlayerIdentity implements Identity {

    @JsonProperty("player")
    public final UUID player;
    @JsonProperty("publicKey")
    public final PublicKey publicKey;

    public PlayerIdentity(@JsonProperty("player") UUID player, @JsonProperty("publicKey") PublicKey publicKey) {
        this.player = player;
        this.publicKey = publicKey;
    }

    @Override
    public UUID id() {
        return player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerIdentity playerIdentity = (PlayerIdentity) o;
        return player.equals(playerIdentity.player) &&
                publicKey.equals(playerIdentity.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, publicKey);
    }
}
