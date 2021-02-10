package team.catgirl.collar.server.services.textures;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.lang3.NotImplementedException;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.Binary;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.HttpException.BadRequestException;
import team.catgirl.collar.api.http.HttpException.ConflictException;
import team.catgirl.collar.api.http.HttpException.ForbiddenException;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.profiles.ProfileService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class TextureService {

    public static final String FIELD_TEXTURE_ID = "textureId";
    public static final String FIELD_BYTES = "bytes";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_OWNER = "owner";
    private final MongoCollection<Document> docs;

    public TextureService(MongoDatabase db) {
        this.docs = db.getCollection("textures");
        Map<String, Object> index = Map.of("owner", 1, "type", 1);
        this.docs.createIndex(new Document(index));
    }

    public CreateTextureResponse createTexture(RequestContext context, CreateTextureRequest request) throws BadRequestException {
        context.assertNotAnonymous();
        if (request.bytes.length > (1024*2)) {
            throw new BadRequestException("bytes must be less than 2mb");
        }
        if (docs.find(and(eq(FIELD_OWNER, request.owner), eq(FIELD_TYPE, request.type.name()))).iterator().hasNext()) {
            throw new ConflictException("owner already has texture of this type");
        }
        Map<String, Object> state = new HashMap<>();
        state.put(FIELD_TEXTURE_ID, UUID.randomUUID());
        state.put(FIELD_BYTES, new Binary(request.bytes));
        state.put(FIELD_TYPE, request.type.name());
        state.put(FIELD_OWNER, context.owner);
        InsertOneResult insertOneResult = docs.insertOne(new Document(state));
        if (insertOneResult.wasAcknowledged()) {
            BsonObjectId id = Objects.requireNonNull(insertOneResult.getInsertedId()).asObjectId();
            MongoCursor<Document> cursor = docs.find(eq("_id", id.getValue())).iterator();
            if (cursor.hasNext()) {
                return new CreateTextureResponse(map(cursor.next()));
            } else {
                throw new HttpException.ServerErrorException("could not find created texture");
            }
        } else {
            throw new HttpException.ServerErrorException("could not create new texture");
        }
    }

    private Texture map(Document doc) {
        UUID id = doc.get(FIELD_TEXTURE_ID, UUID.class);
        String url = "/api/1/textures/" + id.toString() + "/png";
        return new Texture(id, url, TextureType.valueOf(doc.getString(FIELD_TYPE)), doc.get(FIELD_OWNER, UUID.class));
    }

    public GetTextureContentResponse getTextureContent(GetTextureContentRequest req) {
        TextureContent texture = docs.find(and(eq(FIELD_TEXTURE_ID, req.id)))
                .map(doc -> new TextureContent(doc.get(FIELD_TEXTURE_ID, UUID.class), doc.get(FIELD_BYTES, Binary.class).getData()))
                .first();
        if (texture == null) {
            throw new NotImplementedException("cannot find texture " + req.id);
        }
        return new GetTextureContentResponse(texture);
    }

    public FindTextureResponse findTexture(RequestContext context, FindTextureRequest req) {
        context.assertAnonymous();
        Texture texture = docs.find(and(eq(FIELD_OWNER, req.owner), eq(FIELD_TYPE, req.type.name()))).map(this::map).first();
        if (texture == null) {
            throw new NotFoundException("cannot find texture " + req.owner + " " + req.type);
        }
        return new FindTextureResponse(texture);
    }

    public static class CreateTextureRequest {
        public final UUID owner;
        public final TextureType type;
        public final byte[] bytes;

        public CreateTextureRequest(UUID owner, TextureType type, byte[] bytes) {
            this.owner = owner;
            this.type = type;
            this.bytes = bytes;
        }
    }

    public static class CreateTextureResponse {
        public final Texture texture;

        public CreateTextureResponse(Texture texture) {
            this.texture = texture;
        }
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
        public final UUID id;
        public final String url;
        public final TextureType type;
        public final UUID owner;

        public Texture(UUID id, String url, TextureType type, UUID owner) {
            this.id = id;
            this.url = url;
            this.type = type;
            this.owner = owner;
        }
    }

    public static class TextureContent {
        public final UUID id;
        public final byte[] bytes;

        public TextureContent(UUID id, byte[] bytes) {
            this.id = id;
            this.bytes = bytes;
        }
    }

    public static class GetTextureContentRequest {
        public final UUID id;

        public GetTextureContentRequest(UUID id) {
            this.id = id;
        }
    }

    public static class GetTextureContentResponse {
        public final TextureContent content;

        public GetTextureContentResponse(TextureContent content) {
            this.content = content;
        }
    }
}
