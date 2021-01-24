package team.catgirl.collar.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Friends {
    @JsonProperty("items")
    public final Set<Friend> items;

    public Friends(@JsonProperty("items") Set<Friend> items) {
        this.items = items;
    }

    public class Friend {
        @JsonProperty("player")
        public final UUID player;

        public Friend(@JsonProperty("player") UUID player) {
            this.player = player;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Friend that = (Friend) o;
            return player.equals(that.player);
        }

        @Override
        public int hashCode() {
            return Objects.hash(player);
        }
    }
}
