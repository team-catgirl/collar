package team.catgirl.collar.api.waypoints;

import team.catgirl.collar.api.location.Location;

import java.io.*;
import java.util.Objects;
import java.util.UUID;

/**
 * Marks a location in a Minecraft world
 */
public final class Waypoint {
    private static final int VERSION = 1;

    public final UUID id;
    public final String name;
    public final Location location;
    public final String server;

    public Waypoint(UUID id, String name, Location location, String server) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.server = server;
    }

    public Waypoint(byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int serializedVersion = dataStream.readInt();
                switch (serializedVersion) {
                    case 1:
                        id = UUID.fromString(dataStream.readUTF());
                        name = dataStream.readUTF();
                        int locSize = dataStream.readInt();
                        byte[] locationBytes = new byte[locSize];
                        for (int i = 0; i < locSize; i++) {
                            locationBytes[i] = dataStream.readByte();
                        }
                        location = new Location(locationBytes);
                        server = dataStream.readUTF();
                        break;
                    default:
                        throw new IllegalStateException("Unsupported Waypoint version " + serializedVersion);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("could not read waypoint data", e);
        }
    }

    public byte[] serialize() {
        byte[] locationBytes = location.serialize();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.writeInt(VERSION);
                dataStream.writeUTF(id.toString());
                dataStream.writeUTF(name);
                dataStream.writeInt(locationBytes.length);
                for (byte locationByte : locationBytes) {
                    dataStream.writeByte(locationByte);
                }
                dataStream.writeUTF(server);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialize waypoint " + this);
        }
    }

    public String displayName() {
        return name + " " + location.displayName();
    }

    @Override
    public String toString() {
        return name + ":" + id + "@" + location.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Waypoint waypoint = (Waypoint) o;
        return id.equals(waypoint.id) && name.equals(waypoint.name) && location.equals(waypoint.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, location);
    }
}
