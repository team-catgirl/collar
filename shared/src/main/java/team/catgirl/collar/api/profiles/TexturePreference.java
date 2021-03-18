package team.catgirl.collar.api.profiles;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.textures.TextureType;

import java.util.UUID;

public final class TexturePreference {
    @JsonProperty("profile")
    public final UUID profile;
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("type")
    public final TextureType type;

    public TexturePreference(@JsonProperty("profile") UUID profile,
                             @JsonProperty("group") UUID group,
                             @JsonProperty("type") TextureType type) {
        this.profile = profile;
        this.group = group;
        this.type = type;
    }
}
