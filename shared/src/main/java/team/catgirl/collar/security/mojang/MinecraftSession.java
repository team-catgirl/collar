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

    @JsonCreator
    public MinecraftSession(
            @JsonProperty("id") UUID id,
            @JsonProperty("username") String username,
            @JsonProperty("server") String server,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("accessToken") String accessToken) {
        this.id = id;
        this.username = username;
        this.server = server;
        this.mode = mode;
        this.accessToken = accessToken;
    }

    /**
     * @return the minecraft player
     */
    @JsonIgnore
    public MinecraftPlayer toPlayer() {
        return new MinecraftPlayer(id, server.trim());
    }

    /**
     * @param id of the minecraft user
     * @param username of minecraft user
     * @param serverIP of the minecraft server the client is connected to
     * @return minecraft session info
     */
    public static MinecraftSession noJang(UUID id, String username, String serverIP) {
        return new MinecraftSession(id, username, serverIP, Mode.NOJANG, null);
    }

    /**
     * @param id of the minecraft user
     * @param username of minecraft user
     * @param serverIP of the minecraft server the client is connected to
     * @param accessToken of the minecraft session
     * @return minecraft session info
     */
    public static MinecraftSession mojang(UUID id, String username, String serverIP, String accessToken) {
        return new MinecraftSession(id, username, serverIP, Mode.MOJANG, accessToken);
    }

    public enum Mode {
        MOJANG,
        NOJANG
    }
}
