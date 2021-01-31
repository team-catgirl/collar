package team.catgirl.collar.client.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Holds state about the identities Collar profile
 */
public final class IdentityState {
    @JsonProperty("owner")
    public final UUID owner;

    public IdentityState(@JsonProperty("owner") UUID owner) {
        this.owner = owner;
    }

    public void write(HomeDirectory home) {
        try {
            Utils.createObjectMapper().writeValue(new File(home.profile(), "identity.json"), this);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static IdentityState read(HomeDirectory home)  {
        try {
            return Utils.createObjectMapper().readValue(new File(home.security(), "identity.json"), IdentityState.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean exists(HomeDirectory home) {
        try {
            return new File(home.security(), "identity.json").exists();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
