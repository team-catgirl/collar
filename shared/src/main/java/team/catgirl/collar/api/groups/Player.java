package team.catgirl.collar.api.groups;

import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a Collar player
 */
public final class Player {
    public final UUID profile;
    public final MinecraftPlayer minecraftPlayer;

    public Player(UUID profile, MinecraftPlayer minecraftPlayer) {
        this.profile = profile;
        this.minecraftPlayer = minecraftPlayer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return profile.equals(player.profile) && Objects.equals(minecraftPlayer, player.minecraftPlayer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile, minecraftPlayer);
    }
}
