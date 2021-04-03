package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Represents a the session of the Minecraft client
 */
public final class MinecraftSession {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("username")
    public final String username;
    @JsonProperty("server")
    public final String server;
    @JsonProperty("mode")
    public final Mode mode;
    @JsonIgnore
    public final String accessToken;
    @JsonProperty("networkId")
    public final int networkId;

    @JsonCreator
    public MinecraftSession(
            @JsonProperty("id") UUID id,
            @JsonProperty("username") String username,
            @JsonProperty("server") String server,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("accessToken") String accessToken,
            @JsonProperty("networkId") int networkId) {
        this.id = id;
        this.username = username;
        this.server = server;
        this.mode = mode;
        this.accessToken = accessToken;
        this.networkId = networkId;
    }

    /**
     * @return the minecraft player
     */
    @JsonIgnore
    public MinecraftPlayer toPlayer() {
        return new MinecraftPlayer(id, server.trim(), networkId);
    }

    /**
     * @param id of the minecraft user
     * @param username of minecraft user
     * @param networkId of minecraft player
     * @param address of the minecraft server the client is connected to
     * @return minecraft session info
     */
    public static MinecraftSession noJang(UUID id, String username, int networkId, String address) {
        return new MinecraftSession(id, username, address, Mode.NOJANG, null, networkId);
    }

    /**
     * @param id of the minecraft user
     * @param username of minecraft user
     * @param networkId of minecraft user
     * @param address of the minecraft server the client is connected to
     * @param accessToken of the minecraft session
     * @return minecraft session info
     */
    public static MinecraftSession mojang(UUID id, String username, int networkId, String address, String accessToken) {
        return new MinecraftSession(id, username, address, Mode.MOJANG, accessToken, networkId);
    }

    public enum Mode {
        MOJANG,
        NOJANG
    }
}
