package team.catgirl.collar.server.profiles;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import team.catgirl.collar.profiles.PublicProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileService {

    private final MongoCollection<Document> docs;

    public ProfileService(MongoDatabase db) {
        this.docs = db.getCollection("profiles");
        Map<String, Object> index = new HashMap<>();
        index.put("profileId", 1);
        index.put("email", 1);
        docs.createIndex(new Document(index));
    }

    public CreateProfileResponse createProfile(CreateProfileRequest req) {
        throw new IllegalStateException("not implemented");
    }

    public GetProfileResponse getProfile(GetProfileRequest req) {
        return null;
    }

    public static class CreateProfileRequest {
        public final String email;
        public final String password;

        public CreateProfileRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class CreateProfileResponse {
        public final PublicProfile profile;

        public CreateProfileResponse(PublicProfile profile) {
            this.profile = profile;
        }
    }

    public static class GetProfileRequest {
        public final UUID byId;

        public GetProfileRequest(UUID byId) {
            this.byId = byId;
        }

        public static GetProfileRequest byId(UUID uuid) {
            return new GetProfileRequest(uuid);
        }
    }

    public static class GetProfileResponse {
        public final PublicProfile profile;

        public GetProfileResponse(PublicProfile profile) {
            this.profile = profile;
        }
    }
}
