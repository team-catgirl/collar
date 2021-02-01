package team.catgirl.collar.api.location;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Position {
    @JsonProperty("x")
    public final Double x;
    @JsonProperty("y")
    public final Double y;
    @JsonProperty("z")
    public final Double z;
    @JsonProperty("dimension")
    public final Integer dimension;

    public Position(@JsonProperty("x") Double x, @JsonProperty("y") Double y, @JsonProperty("z") Double z, @JsonProperty("dimension") Integer dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    public static String toString(Position location) {
        return location == null ? "" : location.toString();
    }

    @Override
    public String toString() {
        return "[" + x + "," + y + "," + z + "]";
    }
}

