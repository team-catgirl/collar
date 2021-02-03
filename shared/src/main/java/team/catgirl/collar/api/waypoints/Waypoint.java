package team.catgirl.collar.api.waypoints;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Position;

public final class Waypoint {
    @JsonProperty("name")
    public final String name;
    @JsonProperty("position")
    public final Position position;

    public Waypoint(@JsonProperty("name") String name, @JsonProperty("position") Position position) {
        this.name = name;
        this.position = position;
    }
}
