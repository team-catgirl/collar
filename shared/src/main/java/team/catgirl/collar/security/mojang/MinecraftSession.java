package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.chris54721.openmcauthenticator.OpenMCAuthenticator;
import net.chris54721.openmcauthenticator.Profile;
import net.chris54721.openmcauthenticator.exceptions.AuthenticationUnavailableException;
import net.chris54721.openmcauthenticator.exceptions.InvalidCredentialsException;
import net.chris54721.openmcauthenticator.exceptions.RequestException;
import net.chris54721.openmcauthenticator.responses.AuthenticationResponse;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.api.http.HttpException.UnauthorisedException;

import java.util.Arrays;
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

    /**
     * @param id of the minecraft user
     * @param serverIP of the minecraft server the client is connected to
     * @return minecraft session info
     */
    public static MinecraftSession noJang(UUID id, String serverIP) {
        return new MinecraftSession(id, "_", null,  serverIP);
    }
}
