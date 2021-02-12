package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Represents a the session of the Minecraft client
 * This class is used to transmit over the network. As such it should <br>never</br> transmit username and passwords.
 */
public final class MinecraftSession {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("accessToken")
    public final String accessToken;
    @JsonProperty("username")
    public final String username;
    @JsonProperty("server")
    public final String server;

    public MinecraftSession(
            @JsonProperty("id") UUID id,
            @JsonProperty("username") String username,
            @JsonProperty("accessToken") String accessToken,
            @JsonProperty("server") String server) {
        this.id = id;
        this.username = username;
        this.accessToken = accessToken;
        this.server = server;
    }

    /**
     * @return the minecraft player
     */
    @JsonIgnore
    public MinecraftPlayer toPlayer() {
        return new MinecraftPlayer(id, server.trim(), username);
    }

    /**
     * @param id of the minecraft user
     * @param user of the minecraft client
     * @param serverIP of the minecraft server the client is connected to
     * @return minecraft session info
     */
    public static MinecraftSession from(UUID id, String user, String token, String serverIP) {
        return new MinecraftSession(id, user, token, serverIP);
    }
}
