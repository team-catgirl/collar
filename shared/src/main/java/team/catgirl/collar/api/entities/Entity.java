package team.catgirl.collar.api.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public final class Entity {
    public final UUID id;
    public final UUID playerId;
    public final EntityType type;

    public Entity(@JsonProperty("uuid") UUID id,
                  @JsonProperty("playerId") UUID playerId,
                  @JsonProperty("type") EntityType type) {
        this.id = id;
        this.playerId = playerId;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id.equals(entity.id) && Objects.equals(playerId, entity.playerId) && type == entity.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, playerId, type);
    }
}
