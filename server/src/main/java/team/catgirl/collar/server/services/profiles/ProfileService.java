package team.catgirl.collar.server.services.profiles;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.validator.routines.EmailValidator;
import org.bson.BsonObjectId;
import org.bson.Document;
import team.catgirl.collar.api.http.HttpException.BadRequestException;
import team.catgirl.collar.api.http.HttpException.ConflictException;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.security.hashing.PasswordHashing;

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
    private static final String FIELD_EMAIL_VERIFIED = "emailVerified";

    private final MongoCollection<Document> docs;
    private final PasswordHashing passwordHashing;

    public ProfileService(MongoDatabase db, PasswordHashing passwordHashing) {
        this.docs = db.getCollection("profiles");
        Map<String, Object> index = new HashMap<>();
        index.put(FIELD_PROFILE_ID, 1);
        index.put(FIELD_EMAIL, 1);
        docs.createIndex(new Document(index));
        this.passwordHashing = passwordHashing;
    }

    public CreateProfileResponse createProfile(RequestContext context, CreateProfileRequest req) {
        context.assertAnonymous();
        if (docs.find(eq(FIELD_EMAIL, req.email)).iterator().hasNext()) {
            throw new ConflictException("profile with this email already exists");
        }
        if (req.name == null) {
            throw new BadRequestException("name missing");
        }
        if (req.email == null) {
            throw new BadRequestException("email missing");
        }
        if (req.password == null) {
            throw new BadRequestException("password missing");
        }
        if (EmailValidator.getInstance().isValid(req.email)) {
            throw new BadRequestException("email address is invalid");
        }
        String hashedPassword = passwordHashing.hash(req.password);
        Map<String, Object> state = new HashMap<>();
        state.put(FIELD_PROFILE_ID, UUID.randomUUID());
        state.put(FIELD_NAME, req.name);
        state.put(FIELD_EMAIL, req.email.toLowerCase());
        state.put(FIELD_HASHED_PASSWORD, hashedPassword);
        state.put(FIELD_EMAIL_VERIFIED, false);
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

    public UpdateProfileResponse updateProfile(RequestContext context, UpdateProfileRequest req) {
        context.assertNotAnonymous();
        UpdateResult result;
        if (req.emailVerified != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(FIELD_EMAIL_VERIFIED, req.emailVerified)));
        } else if (req.hashedPassword != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(FIELD_HASHED_PASSWORD, req.hashedPassword)));
        } else {
            throw new BadRequestException("bad request");
        }
        if (result.wasAcknowledged()) {
            Document first = docs.find(eq(FIELD_PROFILE_ID, req.profile)).first();
            if (first == null) {
                throw new NotFoundException("could not find profile");
            }
            return new UpdateProfileResponse(map(first));
        } else {
            throw new ServerErrorException("could not update profile");
        }
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

        public GetProfileRequest(UUID byId, String byEmail) {
            this.byId = byId;
            this.byEmail = byEmail;
        }

        public static GetProfileRequest byId(UUID uuid) {
            return new GetProfileRequest(uuid, null);
        }

        public static GetProfileRequest byEmail(String email) {
            return new GetProfileRequest(null, email);
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
        boolean emailVerified = doc.getBoolean(FIELD_EMAIL_VERIFIED);
        return new Profile(profileId, email, name, hashedPassword, emailVerified);
    }

    public static final class UpdateProfileRequest {
        @JsonProperty("profile")
        public final UUID profile;
        @JsonProperty("emailVerified")
        public final Boolean emailVerified;
        @JsonProperty("hashedPassword")
        public final String hashedPassword;

        public UpdateProfileRequest(@JsonProperty("profile") UUID profile,
                                    @JsonProperty("emailVerified") Boolean emailVerified,
                                    @JsonProperty("hashedPassword") String hashedPassword) {
            this.profile = profile;
            this.emailVerified = emailVerified;
            this.hashedPassword = hashedPassword;
        }

        public static UpdateProfileRequest emailVerified(UUID profile) {
            return new UpdateProfileRequest(profile, true, null);
        }

        public static UpdateProfileRequest hashedPassword(UUID profile, String newPassword) {
            return new UpdateProfileRequest(profile, null, newPassword);
        }
    }

    public static final class UpdateProfileResponse {
        @JsonProperty("profile")
        public final Profile profile;

        public UpdateProfileResponse(@JsonProperty("profile") Profile profile) {
            this.profile = profile;
        }
    }
}
