package team.catgirl.collar.server.services.profiles;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonObjectId;
import org.bson.Document;
import team.catgirl.collar.api.http.HttpException.BadRequestException;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.security.KeyPair.PublicKey;
import team.catgirl.collar.server.http.RequestContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public class ProfileService {

    private static final String FIELD_ID = "_id";
    private static final String FIELD_PROFILE_ID = "profileId";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_HASHED_PASSWORD = "hashedPassword";

    private final MongoCollection<Document> docs;

    public ProfileService(MongoDatabase db) {
        this.docs = db.getCollection("profiles");
        Map<String, Object> index = new HashMap<>();
        index.put(FIELD_PROFILE_ID, 1);
        index.put(FIELD_EMAIL, 1);
        docs.createIndex(new Document(index));
    }

    public CreateProfileResponse createProfile(RequestContext context, CreateProfileRequest req) {
        // TODO: use a salt
        String hashedPassword = BCrypt.withDefaults().hashToString(12, req.password.toCharArray());
        Map<String, Object> state = new HashMap<>();
        state.put(FIELD_PROFILE_ID, UUID.randomUUID());
        state.put(FIELD_NAME, req.name);
        state.put(FIELD_EMAIL, req.email.toLowerCase());
        state.put(FIELD_HASHED_PASSWORD, hashedPassword);
        InsertOneResult insertOneResult = docs.insertOne(new Document(state));
        if (insertOneResult.wasAcknowledged()) {
            BsonObjectId id = Objects.requireNonNull(insertOneResult.getInsertedId()).asObjectId();
            MongoCursor<Document> cursor = docs.find(eq(FIELD_ID, id.getValue())).iterator();
            if (cursor.hasNext()) {
                return new CreateProfileResponse(map(cursor.next()));
            } else {
                throw new ServerErrorException("could not find created profile");
            }
        } else {
            throw new ServerErrorException("could not create new profile");
        }
    }

    public GetProfileResponse getProfile(RequestContext context, GetProfileRequest req) {
        context.assertNotAnonymous();
        MongoCursor<Document> cursor;
        if (req.byId != null) {
            cursor = docs.find(eq(FIELD_PROFILE_ID, req.byId)).iterator();
        } else if (req.byEmail != null) {
            cursor = docs.find(eq(FIELD_EMAIL, req.byEmail.toLowerCase())).iterator();
        } else {
            throw new BadRequestException("empty request");
        }
        if (!cursor.hasNext()) {
            throw new NotFoundException("profile not found");
        }
        Document doc = cursor.next();
        return new GetProfileResponse(map(doc));
    }

    public static class CreateProfileRequest {
        public final String email;
        public final String password;
        public final String name;

        public CreateProfileRequest(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }
    }

    public static class CreateProfileResponse {
        public final Profile profile;

        public CreateProfileResponse(Profile profile) {
            this.profile = profile;
        }
    }

    public static class GetProfileRequest {
        public final UUID byId;
        public final String byEmail;
        private final PublicKey byPublicKey;

        public GetProfileRequest(UUID byId, String byEmail, PublicKey byPublicKey) {
            this.byId = byId;
            this.byEmail = byEmail;
            this.byPublicKey = byPublicKey;
        }

        public static GetProfileRequest byId(UUID uuid) {
            return new GetProfileRequest(uuid, null, null);
        }

        public static GetProfileRequest byEmail(String email) {
            return new GetProfileRequest(null, email, null);
        }

        public static GetProfileRequest byPublicKey(PublicKey publicKey) {
            return new GetProfileRequest(null, null, publicKey);
        }
    }

    public static class GetProfileResponse {
        public final Profile profile;

        public GetProfileResponse(Profile profile) {
            this.profile = profile;
        }
    }

    private static Profile map(Document doc) {
        UUID profileId = doc.get(FIELD_PROFILE_ID, UUID.class);
        String email = doc.getString(FIELD_EMAIL);
        String name = doc.getString(FIELD_NAME);
        String hashedPassword = doc.getString(FIELD_HASHED_PASSWORD);
        return new Profile(profileId, email, name, hashedPassword);
    }
}
