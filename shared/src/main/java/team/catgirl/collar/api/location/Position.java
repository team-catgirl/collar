package team.catgirl.collar.api.location;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Position {

    public static final Position UNKNOWN = new Position(Double.MIN_VALUE, Double.MIN_VALUE , Double.MIN_VALUE, Dimension.UNKNOWN);

    @JsonProperty("x")
    public final Double x;
    @JsonProperty("y")
    public final Double y;
    @JsonProperty("z")
    public final Double z;
    @JsonProperty("dimension")
    public final Dimension dimension;

    public Position(@JsonProperty("x") Double x, @JsonProperty("y") Double y, @JsonProperty("z") Double z, @JsonProperty("dimension") Dimension dimension) {
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
        return "[" + x + "," + y + "," + z + "," + dimension + "]";
    }
}

