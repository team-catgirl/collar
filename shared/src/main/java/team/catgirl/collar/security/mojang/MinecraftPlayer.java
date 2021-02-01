package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public final class MinecraftPlayer {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("server")
    public final String server;

    public MinecraftPlayer(@JsonProperty("id") UUID id, @JsonProperty("server") String server) {
        this.id = id;
        this.server = server;
    }

    /**
     * Tests if the players are on the same server
     * @param player to test
     * @return in server or not
     */
    public boolean inServerWith(MinecraftPlayer player) {
        return this.server.equals(player.server);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinecraftPlayer that = (MinecraftPlayer) o;
        return id.equals(that.id) &&
                server.equals(that.server);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, server);
    }

    @Override
    public String toString() {
        return id.toString() + "@" + server;
    }
}
