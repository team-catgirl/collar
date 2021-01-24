package team.catgirl.collar.server.profiles;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import team.catgirl.collar.models.Profile;

public class ProfileService {
    private final MongoCollection docs;

    public ProfileService(MongoDatabase database) {
        this.docs = database.getCollection("profiles");
    }

    public CreateProfileResponse createProfile(CreateProfileRequest profile) {
        throw new RuntimeException("not implemented yet");
    }

    public static class CreateProfileRequest {
    }

    private static class CreateProfileResponse {
    }
}
