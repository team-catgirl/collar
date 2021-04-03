package team.catgirl.collar.api.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public final class Entity {
    @JsonProperty("id")
    public final Integer id;
    @JsonProperty("type")
    public final String type;

    public Entity(@JsonProperty("id") Integer id,
                  @JsonProperty("type") String type) {
        this.id = id;
        this.type = type;
    }
    
    public Entity(final int id, final EntityType type) {
    	this(id, type.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id.equals(entity.id) && type.equals(entity.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}
