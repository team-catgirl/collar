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
    @JsonProperty("clientSession")
    public final String clientToken;
    @JsonProperty("server")
    public final String server;

    public MinecraftSession(
            @JsonProperty("id") UUID id,
            @JsonProperty("accessToken") String accessToken,
            @JsonProperty("clientSession") String clientToken,
            @JsonProperty("server") String server) {
        this.id = id;
        this.accessToken = accessToken;
        this.clientToken = clientToken;
        this.server = server;
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
     * @param accessToken of the minecraft client
     * @param clientToken of the minecraft client
     * @param serverIP of the minecraft server the client is connected to
     * @return minecraft session info
     */
    public static MinecraftSession from(UUID id, String accessToken, String clientToken, String serverIP) {
        return new MinecraftSession(id, accessToken, clientToken, serverIP);
    }

    /**
     * @param id of the minecraft user
     * @param serverIP of the minecraft server the client is connected to
     * @return minecraft session info
     */
    public static MinecraftSession noJang(UUID id, String serverIP) {
        return new MinecraftSession(id, null, null, serverIP);
    }

    /**
     *
     * @param username of minecraft user
     * @param password of minecraft password
     * @param serverIP of the minecraft server the client is connected to
     * @return minecraft session or throws {@link HttpException} on error
     */
    public static MinecraftSession from(String username, String password, String serverIP) {
        AuthenticationResponse resp;
        try {
            resp = OpenMCAuthenticator.authenticate(username, password);
        } catch (InvalidCredentialsException | AuthenticationUnavailableException e) {
            throw new UnauthorisedException("Authenticating with Mojang failed");
        } catch (RequestException e) {
            throw new ServerErrorException("Could not communicate with Mojang", e);
        }
        Profile[] availableProfiles = resp.getAvailableProfiles();
        if (availableProfiles == null || availableProfiles.length == 0) {
            throw new NotFoundException("No profiles found for " + username);
        }
        Profile profile = Arrays.stream(availableProfiles)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No minecraft profile found for " + username));
        return new MinecraftSession(profile.getUUID(), resp.getAccessToken(), resp.getClientToken(), serverIP);
    }
}
