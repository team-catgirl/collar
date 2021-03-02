package team.catgirl.collar.client.api.textures;

import io.mikael.urlbuilder.UrlBuilder;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.textures.GetTextureRequest;
import team.catgirl.collar.protocol.textures.GetTextureResponse;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TexturesApi extends AbstractApi<TexturesListener> {

    public TexturesApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof GetTextureResponse) {
            GetTextureResponse response = (GetTextureResponse) resp;
            Texture texture = new Texture(response.player, response.group, response.type, UrlBuilder.fromUrl(collar.configuration.collarServerURL).withPath(response.texturePath).toUrl());
            fireListener("onTextureReceived", texturesListener -> {
                texturesListener.onTextureReceived(collar, this, texture);
            });
            return true;
        }
        return false;
    }

    public void requestPlayerTexture(Player player, TextureType type) {
        sender.accept(new GetTextureRequest(identity(), player.profile, null, type));
    }


    public void requestGroupTexture(Group group, TextureType type) {
        sender.accept(new GetTextureRequest(identity(), null, group.id, type));
    }

    @Override
    public void onStateChanged(Collar.State state) {}
}
