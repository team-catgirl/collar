package team.catgirl.collar.api.profiles;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class TexturePreference {
    public final UUID texture;

    public TexturePreference(@JsonProperty("texture") UUID texture) {
        this.texture = texture;
    }
}
