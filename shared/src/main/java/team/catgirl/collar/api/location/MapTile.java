package team.catgirl.collar.api.location;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;

public final class MapTile {
    private static final int MAX_SIZE = 200;

    @JsonProperty("point")
    public final Point point;
    @JsonProperty("dimension")
    public final Dimension dimension;
    @JsonProperty("layer")
    public final Layer layer;
    @JsonProperty("image")
    public final byte[] image;

    public MapTile(@JsonProperty("point") Point point,
                   @JsonProperty("dimension") Dimension dimension,
                   @JsonProperty("layer") Layer layer,
                   @JsonProperty("image") byte[] image) {
        this.layer = layer;
        if (image != null) {
            if (image.length > MAX_SIZE) {
                throw new IllegalArgumentException("max size of map tile is 200 bytes");
            }
            if (image.length >= 12
                    && !new String(image, 0, 4, StandardCharsets.UTF_8).equals("RIFF")
                    && !new String(image, 5, 4, StandardCharsets.UTF_8).equals("WEBP")) {
                throw new IllegalArgumentException("image is not webp");
            }
        }
        this.point = point;
        this.dimension = dimension;
        this.image = image;
    }
}
