package team.catgirl.collar.server.services.location;

import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.*;

/**
 * Represents players that appear in each others entity lists that should be placed in a group together
 */
public final class NearbyGroup {
    public final Set<MinecraftPlayer> players;

    public NearbyGroup(Set<MinecraftPlayer> players) {
        this.players = players;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NearbyGroup that = (NearbyGroup) o;
        return players.equals(that.players);
    }

    @Override
    public int hashCode() {
        return Objects.hash(players);
    }
}
