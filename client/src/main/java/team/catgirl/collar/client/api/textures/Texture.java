package team.catgirl.collar.client.api.textures;

import io.mikael.urlbuilder.UrlBuilder;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.client.utils.Http;
import team.catgirl.collar.http.Request;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Client representation of a remote Texture
 */
public final class Texture {

    public final Player player;
    public final UUID group;
    public final TextureType type;
    private final URL url;

    public Texture(Player player, UUID group, TextureType type, URL url) {
        this.player = player;
        this.group = group;
        this.type = type;
        this.url = url;
    }

    /**
     * Async loads the remote texture as a {@link BufferedImage}
     * @param onLoad accepts null if error or image if successful
     */
    public void loadImage(Consumer<Optional<BufferedImage>> onLoad) {
        ForkJoinPool.commonPool().submit(() -> {
            try {
                byte[] bytes = Http.collar().bytes(Request.url(UrlBuilder.fromUrl(url)).get());
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                onLoad.accept(Optional.of(image));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
