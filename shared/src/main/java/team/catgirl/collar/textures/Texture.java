package team.catgirl.collar.textures;

import java.util.UUID;

public final class Texture {
    public final UUID owner;
    public final Type type;
    public final byte[] bytes;

    public Texture(UUID owner, Type type, byte[] bytes) {
        this.owner = owner;
        this.type = type;
        this.bytes = bytes;
    }

    public enum Type {
        CAPE
    }
}
