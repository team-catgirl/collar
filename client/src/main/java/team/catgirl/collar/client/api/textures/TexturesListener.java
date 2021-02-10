package team.catgirl.collar.client.api.textures;

import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.features.ApiListener;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public interface TexturesListener extends ApiListener {
    void onPlayerTextureReceived(Collar collar, TexturesApi texturesApi, MinecraftPlayer player, Texture texture);
}
