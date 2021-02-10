package team.catgirl.collar.protocol.textures;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

/**
 * Sent when the client requests the texture
 */
public final class GetTextureRequest extends ProtocolRequest {
    @JsonProperty("player")
    public final UUID player;
    @JsonProperty("type")
    public final TextureType type;

    public GetTextureRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("player") UUID player, @JsonProperty("type") TextureType type) {
        super(identity);
        this.player = player;
        this.type = type;
    }
}
