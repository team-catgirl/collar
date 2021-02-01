package team.catgirl.collar.api.profiles;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class PublicProfile {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("name")
    public final String name;

    public PublicProfile(@JsonProperty("id") UUID id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
