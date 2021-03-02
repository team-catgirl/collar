package team.catgirl.collar.client.api.textures;

import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.ApiListener;

public interface TexturesListener extends ApiListener {
    void onTextureReceived(Collar collar, TexturesApi texturesApi, Texture texture);
}
