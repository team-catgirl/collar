package team.catgirl.collar.server.services.profiles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.profiles.PublicProfile;

import java.util.UUID;

public final class Profile {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("email")
    public final String email;
    @JsonProperty("name")
    public final String name;
    @JsonIgnore
    public final String hashedPassword;

    public Profile(UUID id, String email, String name, String hashedPassword) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.hashedPassword = hashedPassword;
    }

    @JsonCreator
    public Profile(@JsonProperty("id") UUID id, @JsonProperty("email") String email, @JsonProperty("name") String name) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.hashedPassword = null;
    }

    @JsonIgnore
    public PublicProfile toPublic() {
        return new PublicProfile(this.id, this.name);
    }
}
