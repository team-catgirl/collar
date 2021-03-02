package team.catgirl.collar.api.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a Collar player
 */
public final class Player {
    @JsonProperty("profile")
    public final UUID profile;
    @JsonProperty("minecraftPlayer")
    public final MinecraftPlayer minecraftPlayer;

    public Player(@JsonProperty("profile") UUID profile,
                  @JsonProperty("minecraftPlayer") MinecraftPlayer minecraftPlayer) {
        this.profile = profile;
        this.minecraftPlayer = minecraftPlayer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return profile.equals(player.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile);
    }

    @Override
    public String toString() {
        return profile + ":" + minecraftPlayer;
    }
}
