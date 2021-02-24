package team.catgirl.collar.api.location;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.*;
import java.util.Objects;

public final class Location {

    private static final int VERSION = 1;
    public static final Location UNKNOWN = new Location(Double.MIN_VALUE, Double.MIN_VALUE , Double.MIN_VALUE, Dimension.UNKNOWN);

    @JsonProperty("x")
    public final Double x;
    @JsonProperty("y")
    public final Double y;
    @JsonProperty("z")
    public final Double z;
    @JsonProperty("dimension")
    public final Dimension dimension;

    public Location(byte[] bytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int serializedVersion = dataStream.readInt();
                switch (serializedVersion) {
                    case 1:
                        x = dataStream.readDouble();
                        y = dataStream.readDouble();
                        z = dataStream.readDouble();
                        dimension = Dimension.valueOf(dataStream.readUTF());
                        break;
                    default:
                        throw new IllegalStateException("unsupported Location version " + serializedVersion);
                }
            }
        }
    }

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

    public byte[] serialize() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.writeInt(VERSION);
                dataStream.writeDouble(x);
                dataStream.writeDouble(y);
                dataStream.writeDouble(z);
                dataStream.writeUTF(dimension.name());
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialize Location " + this, e);
        }
    }
}

