package team.catgirl.collar.server.services.textures;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.server.http.RequestContext;

import java.util.UUID;

public class TextureService {

    private final MongoCollection<Document> docs;

    public TextureService(MongoDatabase db) {
        this.docs = db.getCollection("textures");
    }

    public UpsertTextureResponse upsertTexture(RequestContext context, UpsertTextureRequest request) {
        return null;
    }

    public FindTextureResponse findTexture(RequestContext context, FindTextureRequest req) {
        return null;
    }

    public static class UpsertTextureRequest {
        public final UUID owner;
        public final TextureType type;
        public final byte[] bytes;

        public UpsertTextureRequest(UUID owner, TextureType type, byte[] bytes) {
            this.owner = owner;
            this.type = type;
            this.bytes = bytes;
        }
    }

    public static class UpsertTextureResponse {

    }

    public static class FindTextureRequest {
        public final UUID owner;
        public final TextureType type;

        public FindTextureRequest(UUID owner, TextureType type) {
            this.owner = owner;
            this.type = type;
        }
    }

    public static class FindTextureResponse {
        public final Texture texture;

        public FindTextureResponse(Texture texture) {
            this.texture = texture;
        }
    }

    public static class Texture {
        public final UUID textureId;
        public final String url;
        public final TextureType type;
        public final UUID owner;

        public Texture(UUID textureId, String url, TextureType type, UUID owner) {
            this.textureId = textureId;
            this.url = url;
            this.type = type;
            this.owner = owner;
        }
    }
}
