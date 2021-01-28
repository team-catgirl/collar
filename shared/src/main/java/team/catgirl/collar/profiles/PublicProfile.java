package team.catgirl.collar.profiles;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class PublicProfile {
    public final UUID id;
    public final String name;

    public PublicProfile(@JsonProperty("id") UUID id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
