package team.catgirl.collar.server.profiles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import team.catgirl.collar.profiles.PublicProfile;

import java.util.UUID;

public final class Profile {
    public final UUID id;
    public final String email;
    public final String name;
    @JsonIgnore
    public final String password;

    public Profile(UUID id, String email, String name, String password) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.password = password;
    }

    public PublicProfile toPublic() {
        return new PublicProfile(this.id, this.name);
    }
}
