package team.catgirl.collar.server.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;

public final class Mongo {
    private static MongoDatabase database;

    public static MongoDatabase database() {
        if (database == null) {
            String mongoUrl = System.getenv("MONGODB_URI");
            database = mongoUrl == null ? getDevelopmentDatabase() : database(mongoUrl);
        }
        return database;
    }

    public static MongoDatabase database(String mongoUrl) {
        ConnectionString uri = new ConnectionString(mongoUrl);
        MongoClient mongoClient = MongoClients.create(settings(uri));
        String db = uri.getDatabase();
        if (db == null) {
            throw new IllegalStateException("MONGODB_URI did not include database name");
        }
        return mongoClient.getDatabase(db);
    }

    private static MongoClientSettings settings(ConnectionString uri) {
        return MongoClientSettings.builder()
                .applyConnectionString(uri)
                .uuidRepresentation(UuidRepresentation.STANDARD).build();
    }

    private static MongoDatabase getDevelopmentDatabase() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .uuidRepresentation(UuidRepresentation.STANDARD).build();
        return MongoClients.create(settings).getDatabase("collar-dev");
    }

    public static MongoDatabase getTestingDatabase() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .uuidRepresentation(UuidRepresentation.STANDARD).build();
        return MongoClients.create(settings).getDatabase("collar-testing");
    }

    private Mongo() {}
}
