package team.catgirl.collar.api.textures;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum TextureType {
    AVATAR,
    CAPE,
    @JsonEnumDefaultValue
    UNKNOWN
}
