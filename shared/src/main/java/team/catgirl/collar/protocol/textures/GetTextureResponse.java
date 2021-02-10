package team.catgirl.collar.protocol.textures;

import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.UUID;

public class GetTextureResponse extends ProtocolResponse {
    public final UUID textureId;
    public final MinecraftPlayer player;
    public final String texturePath;
    public final TextureType type;

    public GetTextureResponse(ServerIdentity identity, UUID textureId, MinecraftPlayer player, String texturePath, TextureType type) {
        super(identity);
        this.textureId = textureId;
        this.player = player;
        this.texturePath = texturePath;
        this.type = type;
    }
}
