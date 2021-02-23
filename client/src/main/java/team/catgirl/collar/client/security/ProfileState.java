package team.catgirl.collar.client.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Holds state about the authorized identity's Collar profile
 */
public final class ProfileState {
    @JsonProperty("owner")
    public final UUID owner;

    public ProfileState(@JsonProperty("owner") UUID owner) {
        this.owner = owner;
    }

    public void write(HomeDirectory home) {
        try {
            Utils.messagePackMapper().writeValue(new File(home.profile(), "profile"), this);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ProfileState read(HomeDirectory home)  {
        try {
            return Utils.messagePackMapper().readValue(new File(home.profile(), "profile"), ProfileState.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean exists(HomeDirectory home) {
        try {
            return new File(home.profile(), "profile").exists();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
