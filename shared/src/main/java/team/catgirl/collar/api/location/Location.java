package team.catgirl.collar.api.location;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public final class Location {

    public static final Location UNKNOWN = new Location(Double.MIN_VALUE, Double.MIN_VALUE , Double.MIN_VALUE, Dimension.UNKNOWN);

    @JsonProperty("x")
    public final Double x;
    @JsonProperty("y")
    public final Double y;
    @JsonProperty("z")
    public final Double z;
    @JsonProperty("dimension")
    public final Dimension dimension;

    public Location(@JsonProperty("x") Double x, @JsonProperty("y") Double y, @JsonProperty("z") Double z, @JsonProperty("dimension") Dimension dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return x.equals(location.x) && y.equals(location.y) && z.equals(location.z) && dimension == location.dimension;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, dimension);
    }

    public static String toString(Location location) {
        return location == null ? "" : location.toString();
    }

    @Override
    public String toString() {
        return "[" + x + "," + y + "," + z + "," + dimension + "]";
    }
}

