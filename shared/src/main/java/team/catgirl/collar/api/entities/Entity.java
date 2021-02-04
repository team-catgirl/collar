package team.catgirl.collar.api.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class Entity {
    public final UUID id;
    public final UUID playerId;
    public final EntityType type;

    public Entity(@JsonProperty("uuid") UUID id, @JsonProperty("playerId") UUID playerId, @JsonProperty("type") EntityType type) {
        this.id = id;
        this.playerId = playerId;
        this.type = type;
    }
}
