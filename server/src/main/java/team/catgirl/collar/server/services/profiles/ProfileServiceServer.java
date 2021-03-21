package team.catgirl.collar.server.services.profiles;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.validator.routines.EmailValidator;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.Binary;
import team.catgirl.collar.api.http.HttpException.BadRequestException;
import team.catgirl.collar.api.http.HttpException.ConflictException;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.api.profiles.ProfileService;
import team.catgirl.collar.api.profiles.Role;
import team.catgirl.collar.api.profiles.TexturePreference;
import team.catgirl.collar.server.security.hashing.PasswordHashing;
import team.catgirl.collar.api.http.RequestContext;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.push;

public class ProfileServiceServer implements ProfileService {

    private static final String FIELD_ID = "_id";
    private static final String FIELD_PROFILE_ID = "profileId";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_HASHED_PASSWORD = "hashedPassword";
    private static final String FIELD_EMAIL_VERIFIED = "emailVerified";
    private static final String FIELD_PRIVATE_IDENTITY_TOKEN = "privateIdentityToken";
    private static final String FIELD_CAPE_TEXTURE = "capeTexture";
    private static final String FIELD_CAPE_TEXTURE_ID = "texture";
    private static final String FIELD_KNOWN_ACCOUNTS = "knownAccounts";
    private static final String FIELD_ROLES = "roles";

    private final MongoCollection<Document> docs;
    private final PasswordHashing passwordHashing;

    public ProfileServiceServer(MongoDatabase db, PasswordHashing passwordHashing) {
        this.docs = db.getCollection("profiles");
        Map<String, Object> index = new HashMap<>();
        index.put(FIELD_PROFILE_ID, 1);
        index.put(FIELD_EMAIL, 1);
        docs.createIndex(new Document(index));
        this.passwordHashing = passwordHashing;
    }

    @Override
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
        if (!EmailValidator.getInstance().isValid(req.email)) {
            throw new BadRequestException("email address is invalid");
        }
        String hashedPassword = passwordHashing.hash(req.password);
        Map<String, Object> state = new HashMap<>();
        state.put(FIELD_PROFILE_ID, UUID.randomUUID());
        state.put(FIELD_NAME, req.name);
        state.put(FIELD_EMAIL, req.email.toLowerCase());
        state.put(FIELD_HASHED_PASSWORD, hashedPassword);
        state.put(FIELD_EMAIL_VERIFIED, false);
        state.put(FIELD_ROLES, List.of(Role.PLAYER.name()));
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

    @Override
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

    @Override
    public UpdateProfileResponse updateProfile(RequestContext context, UpdateProfileRequest req) {
        context.assertNotAnonymous();
        UpdateResult result;
        if (req.emailVerified != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(FIELD_EMAIL_VERIFIED, req.emailVerified)));
        } else if (req.hashedPassword != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(FIELD_HASHED_PASSWORD, req.hashedPassword)));
        } else if (req.privateIdentityToken != null) {
            Binary token = req.privateIdentityToken.length == 0 ? null : new Binary(req.privateIdentityToken);
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(FIELD_PRIVATE_IDENTITY_TOKEN, token)));
        } else if (req.cape != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(FIELD_CAPE_TEXTURE, map(req.cape))));
        } else if (req.addMinecraftAccount != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), push(FIELD_KNOWN_ACCOUNTS, req.addMinecraftAccount));
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

    private static Profile map(Document doc) {
        UUID profileId = doc.get(FIELD_PROFILE_ID, UUID.class);
        String email = doc.getString(FIELD_EMAIL);
        String name = doc.getString(FIELD_NAME);
        String hashedPassword = doc.getString(FIELD_HASHED_PASSWORD);
        boolean emailVerified = doc.getBoolean(FIELD_EMAIL_VERIFIED, false);
        Binary privateIdentityToken = doc.get(FIELD_PRIVATE_IDENTITY_TOKEN, Binary.class);
        TexturePreference capeTexture = mapTexturePreference(doc.get(FIELD_CAPE_TEXTURE, Document.class));
        List<UUID> knownAccounts = doc.getList("knownAccounts", UUID.class);
        Set<Role> roles = doc.getList(FIELD_ROLES, String.class, List.of()).stream().map(Role::valueOf).collect(Collectors.toSet());
        return new Profile(
                profileId,
                roles,
                email,
                name,
                hashedPassword,
                emailVerified,
                privateIdentityToken != null ? privateIdentityToken.getData() : null,
                capeTexture,
                knownAccounts == null ? Set.of() : Set.copyOf(knownAccounts)
        );
    }

    private static TexturePreference mapTexturePreference(Document doc) {
        if (doc == null) {
            return null;
        }
        return new TexturePreference(doc.get(FIELD_CAPE_TEXTURE_ID, UUID.class));
    }

    private Document map(TexturePreference capeTexture) {
        return new Document(Map.of(
            FIELD_CAPE_TEXTURE_ID, capeTexture.texture
        ));
    }
}
