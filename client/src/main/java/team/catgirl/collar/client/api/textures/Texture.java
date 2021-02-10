package team.catgirl.collar.client.api.textures;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client representation of a remote Texture
 * Use {@link TexturesApi#requestPlayerTexture(UUID, TextureType)} to be handed one through {@link TexturesListener}
 */
public final class Texture {

    private final Logger LOGGER = Logger.getLogger(Texture.class.getName());

    public final MinecraftPlayer player;
    public final TextureType type;
    private final URL url;

    public Texture(MinecraftPlayer player, TextureType type, URL url) {
        this.player = player;
        this.type = type;
        this.url = url;
    }

    /**
     * Async loads the remote texture as a {@link BufferedImage}
     * @param onLoad accepts null if error or image if successful
     */
    public void loadImage(Consumer<BufferedImage> onLoad) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Utils.http().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load texture from " + url, e);
                onLoad.accept(null);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null || !response.isSuccessful()) {
                        LOGGER.log(Level.SEVERE, "Failed to load texture from " + url);
                        onLoad.accept(null);
                        return;
                    }
                    InputStream is = responseBody.byteStream();
                    BufferedImage image = ImageIO.read(is);
                    onLoad.accept(image);
                }
            }
        });
    }
}
