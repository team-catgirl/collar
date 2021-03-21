package team.catgirl.collar.server.services.textures;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.lang3.NotImplementedException;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.Binary;
import team.catgirl.collar.api.http.HttpException.BadRequestException;
import team.catgirl.collar.api.http.HttpException.ConflictException;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.api.http.RequestContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class TextureService {

    public static final String FIELD_TEXTURE_ID = "textureId";
    public static final String FIELD_TEXTURE_GROUP = "groupId";
    public static final String FIELD_ID = "_id";
    public static final String FIELD_BYTES = "bytes";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_OWNER = "owner";
    private final MongoCollection<Document> docs;

    public TextureService(MongoDatabase db) {
        this.docs = db.getCollection("textures");
        Map<String, Object> index = Map.of(FIELD_OWNER, 1, FIELD_TYPE, 1);
        this.docs.createIndex(new Document(index));
    }

    public CreateTextureResponse createTexture(RequestContext context, CreateTextureRequest request) throws BadRequestException {
        context.assertNotAnonymous();
        if (request.bytes.length > (2*1e+6)) {
            throw new BadRequestException("bytes must be less than 2mb");
        }
        if (request.profile != null && request.group != null) {
            throw new BadRequestException("cannot set both profile and group");
        }
        if (docs.find(and(eq(FIELD_OWNER, request.profile), eq(FIELD_TYPE, request.type.name()))).iterator().hasNext()) {
            throw new ConflictException("owner already has texture of this type");
        }
        Map<String, Object> state = new HashMap<>();
        state.put(FIELD_TEXTURE_ID, UUID.randomUUID());
        state.put(FIELD_BYTES, new Binary(request.bytes));
        state.put(FIELD_TYPE, request.type.name());
        state.put(FIELD_OWNER, context.owner);
        state.put(FIELD_TEXTURE_GROUP, request.group);
        InsertOneResult insertOneResult = docs.insertOne(new Document(state));
        if (insertOneResult.wasAcknowledged()) {
            BsonObjectId id = Objects.requireNonNull(insertOneResult.getInsertedId()).asObjectId();
            MongoCursor<Document> cursor = docs.find(eq(FIELD_ID, id.getValue())).iterator();
            if (cursor.hasNext()) {
                return new CreateTextureResponse(map(cursor.next()));
            } else {
                throw new ServerErrorException("could not find created texture");
            }
        } else {
            throw new ServerErrorException("could not create new texture");
        }
    }

    private Texture map(Document doc) {
        UUID id = doc.get(FIELD_TEXTURE_ID, UUID.class);
        UUID group = doc.get(FIELD_TEXTURE_GROUP, UUID.class);
        String url = "/api/1/textures/" + id.toString() + "/png";
        return new Texture(id, url, TextureType.valueOf(doc.getString(FIELD_TYPE)), doc.get(FIELD_OWNER, UUID.class), group);
    }

    public FindTexturesResponse findTextures(RequestContext context, FindTexturesRequest req) {
        if (req.type == null) {
            throw new BadRequestException("missing type");
        }
        MongoCursor<Texture> iterator;
        if (req.group != null) {
            iterator = docs.find(and(eq(FIELD_TYPE, req.type.name()), eq(FIELD_TEXTURE_GROUP, req.group))).map(this::map).iterator();
        } else if (req.profile != null) {
            iterator = docs.find(and(eq(FIELD_TYPE, req.type.name()), eq(FIELD_OWNER, req.profile))).map(this::map).iterator();
        } else {
            throw new IllegalStateException("missing group or profile");
        }
        return new FindTexturesResponse(StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false).collect(Collectors.toList()));
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

    public GetTextureResponse getTexture(RequestContext context, GetTextureRequest req) {
        context.assertAnonymous();
        Texture texture;
        if (req.texture != null) {
            texture = docs.find(and(eq(FIELD_OWNER, req.profile), eq(FIELD_TEXTURE_ID, req.texture))).map(this::map).first();
        } else if (req.profile != null) {
            texture = docs.find(and(eq(FIELD_OWNER, req.profile), eq(FIELD_TYPE, req.type.name()))).map(this::map).first();
        } else if (req.group != null) {
            texture = docs.find(and(eq(FIELD_TEXTURE_GROUP, req.group), eq(FIELD_TYPE, req.type.name()))).map(this::map).first();
        } else {
            throw new BadRequestException("profile or group not set");
        }
        if (texture == null) {
            throw new NotFoundException("cannot find texture " + req.profile + " " + req.type);
        }
        return new GetTextureResponse(texture);
    }

    public static class CreateTextureRequest {
        @JsonProperty("profile")
        public final UUID profile;
        @JsonProperty("group")
        public final UUID group;
        @JsonProperty("type")
        public final TextureType type;
        @JsonProperty("byte")
        public final byte[] bytes;

        public CreateTextureRequest(@JsonProperty("profile") UUID profile,
                                    @JsonProperty("group") UUID group,
                                    @JsonProperty("type") TextureType type,
                                    @JsonProperty("byte") byte[] bytes) {
            this.profile = profile;
            this.group = group;
            this.type = type;
            this.bytes = bytes;
        }
    }

    public static class CreateTextureResponse {
        @JsonProperty("texture")
        public final Texture texture;

        public CreateTextureResponse(@JsonProperty("texture") Texture texture) {
            this.texture = texture;
        }
    }

    public static class GetTextureRequest {
        @JsonProperty("texture")
        public final UUID texture;
        @JsonProperty("profile")
        public final UUID profile;
        @JsonProperty("group")
        public final UUID group;
        @JsonProperty("type")
        public final TextureType type;

        public GetTextureRequest(@JsonProperty("texture") UUID texture,
                                 @JsonProperty("profile") UUID profile,
                                 @JsonProperty("group") UUID group,
                                 @JsonProperty("type") TextureType type) {
            this.texture = texture;
            this.profile = profile;
            this.group = group;
            this.type = type;
        }
    }

    public static class GetTextureResponse {
        public final Texture texture;

        public GetTextureResponse(@JsonProperty("texture") Texture texture) {
            this.texture = texture;
        }
    }

    public static final class Texture {
        @JsonProperty("id")
        public final UUID id;
        @JsonProperty("url")
        public final String url;
        @JsonProperty("type")
        public final TextureType type;
        @JsonProperty("profile")
        public final UUID profile;
        @JsonProperty("group")
        public final UUID group;

        public Texture(@JsonProperty("id") UUID id,
                       @JsonProperty("url") String url,
                       @JsonProperty("type") TextureType type,
                       @JsonProperty("profile") UUID profile,
                       @JsonProperty("group") UUID group) {
            this.id = id;
            this.url = url;
            this.type = type;
            this.profile = profile;
            this.group = group;
        }
    }

    public static class TextureContent {
        @JsonProperty("id")
        public final UUID id;
        @JsonProperty("bytes")
        public final byte[] bytes;

        public TextureContent(@JsonProperty("id") UUID id,
                              @JsonProperty("bytes") byte[] bytes) {
            this.id = id;
            this.bytes = bytes;
        }
    }

    public static class GetTextureContentRequest {
        @JsonProperty("id")
        public final UUID id;

        public GetTextureContentRequest(@JsonProperty("id") UUID id) {
            this.id = id;
        }
    }

    public static class GetTextureContentResponse {
        @JsonProperty("content")
        public final TextureContent content;

        public GetTextureContentResponse(@JsonProperty("content") TextureContent content) {
            this.content = content;
        }
    }

    public static final class FindTexturesRequest {
        public final TextureType type;
        public final UUID profile;
        public final UUID group;

        public FindTexturesRequest(@JsonProperty("type") TextureType type,
                                   @JsonProperty("profile") UUID profile,
                                   @JsonProperty("group") UUID group) {
            this.type = type;
            this.profile = profile;
            this.group = group;
        }
    }

    public static final class FindTexturesResponse {
        public final List<Texture> textures;

        public FindTexturesResponse(@JsonProperty("textures") List<Texture> textures) {
            this.textures = textures;
        }
    }
}
