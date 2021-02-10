package team.catgirl.collar.protocol.textures;

import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public class GetTextureRequest extends ProtocolRequest {
    public final UUID player;
    public final TextureType type;

    public GetTextureRequest(ClientIdentity identity, UUID player, TextureType type) {
        super(identity);
        this.player = player;
        this.type = type;
    }
}
