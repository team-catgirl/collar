package team.catgirl.collar.api.profiles;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class PublicProfile {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("cape")
    public final TexturePreference cape;

    public PublicProfile(@JsonProperty("id") UUID id,
                         @JsonProperty("name") String name,
                         @JsonProperty("cape") TexturePreference cape) {
        this.id = id;
        this.name = name;
        this.cape = cape;
    }
}
