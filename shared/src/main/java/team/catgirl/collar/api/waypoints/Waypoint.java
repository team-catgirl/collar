package team.catgirl.collar.api.waypoints;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Position;

import java.util.UUID;

public final class Waypoint {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("position")
    public final Position position;

    public Waypoint(@JsonProperty("id") UUID id, @JsonProperty("name") String name, @JsonProperty("position") Position position) {
        this.id = id;
        this.name = name;
        this.position = position;
    }
}
