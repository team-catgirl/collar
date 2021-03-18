package team.catgirl.collar.client.api.textures;

import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import com.stoyanr.evictor.map.ConcurrentHashMapWithTimedEviction;
import com.stoyanr.evictor.map.EvictibleEntry;
import com.stoyanr.evictor.scheduler.RegularTaskEvictionScheduler;
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

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TexturesApi extends AbstractApi<TexturesListener> {

    private final RegularTaskEvictionScheduler<TextureKey, CompletableFuture<Optional<Texture>>> texturesFutureScheduler = new RegularTaskEvictionScheduler<TextureKey, CompletableFuture<Optional<Texture>>>(1, TimeUnit.SECONDS) {
        @Override
        protected void onScheduleEviction(EvictibleEntry<TextureKey, CompletableFuture<Optional<Texture>>> entry) {
            super.onScheduleEviction(entry);
            entry.getValue().complete(null);
        }
    };

    private final ConcurrentMapWithTimedEviction<TextureKey, CompletableFuture<Optional<Texture>>> textureFutures = new ConcurrentHashMapWithTimedEviction<>(texturesFutureScheduler);

    public TexturesApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof GetTextureResponse) {
            GetTextureResponse response = (GetTextureResponse) resp;
                TextureKey textureKey;
            if (response.player != null) {
                textureKey = new TextureKey(response.player.minecraftPlayer.id, response.type);
            } else if (response.group != null) {
                textureKey = new TextureKey(response.group, response.type);
            } else {
                throw new IllegalStateException("neither group or player texture was returned");
            }
            URL textureUrl = UrlBuilder.fromUrl(collar.configuration.collarServerURL).withPath(response.texturePath).toUrl();
            Texture texture = response.texturePath == null ? null : new Texture(response.player, response.group, response.type, textureUrl);
            CompletableFuture<Optional<Texture>> removed = textureFutures.remove(textureKey);
            if (removed != null) {
                Optional<Texture> optionalTexture = texture == null ? Optional.empty() : Optional.of(texture);
                removed.complete(optionalTexture);
            }
            if (texture != null) {
                fireListener("onTextureReceived", texturesListener -> {
                    texturesListener.onTextureReceived(collar, this, texture);
                });
            }
            return true;
        }
        return false;
    }

    /**
     * Request a player texture by future
     * @param player the texture belongs to
     * @param type the type of texture
     * @return future
     */
    public CompletableFuture<Optional<Texture>> playerTextureFuture(Player player, TextureType type) {
        CompletableFuture<Optional<Texture>> future = new CompletableFuture<>();
        textureFutures.put(new TextureKey(player.minecraftPlayer.id, type), future);
        requestPlayerTexture(player, type);
        return future;
    }

    /**
     * Request a player texture
     * @param player the texture belongs to
     * @param type the type of texture
     */
    public void requestPlayerTexture(Player player, TextureType type) {
        sender.accept(new GetTextureRequest(identity(), player.minecraftPlayer.id, null, type));
    }

    /**
     * Request a group texture by future
     * @param group the texture belongs to
     * @param type the type of texture
     * @return future
     */
    public CompletableFuture<Optional<Texture>> groupTextureFuture(Group group, TextureType type) {
        CompletableFuture<Optional<Texture>> future = new CompletableFuture<>();
        textureFutures.put(new TextureKey(group.id, type), future);
        requestGroupTexture(group, type);
        return future;
    }

    /**
     * Request a group texture
     * @param group the texture belongs to
     * @param type the type of texture
     */
    public void requestGroupTexture(Group group, TextureType type) {
        sender.accept(new GetTextureRequest(identity(), null, group.id, type));
    }

    @Override
    public void onStateChanged(Collar.State state) {}

    private static final class TextureKey {
        public final UUID id;
        public final TextureType type;

        public TextureKey(UUID id, TextureType type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TextureKey that = (TextureKey) o;
            return id.equals(that.id) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, type);
        }
    }
}
