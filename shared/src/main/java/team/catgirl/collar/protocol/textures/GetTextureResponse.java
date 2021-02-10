package team.catgirl.collar.protocol.textures;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.UUID;

/**
 * Sent back to the client in response to {@link GetTextureRequest}
 */
public final class GetTextureResponse extends ProtocolResponse {
    @JsonProperty("textureId")
    public final UUID textureId;
    @JsonProperty("player")
    public final MinecraftPlayer player;
    @JsonProperty("texturePath")
    public final String texturePath;
    @JsonProperty("type")
    public final TextureType type;

    public GetTextureResponse(@JsonProperty("identity") ServerIdentity identity,
                              @JsonProperty("textureId") UUID textureId,
                              @JsonProperty("player") MinecraftPlayer player,
                              @JsonProperty("texturePath") String texturePath,
                              @JsonProperty("type") TextureType type) {
        super(identity);
        this.textureId = textureId;
        this.player = player;
        this.texturePath = texturePath;
        this.type = type;
    }
}
