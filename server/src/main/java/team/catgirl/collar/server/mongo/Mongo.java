package team.catgirl.collar.server.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
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
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY)
                .uuidRepresentation(UuidRepresentation.STANDARD).build();
    }

    private static MongoDatabase getDevelopmentDatabase() {
        MongoClientSettings settings = defaultSettings();
        return MongoClients.create(settings).getDatabase("collar-dev");
    }

    public static MongoDatabase getTestingDatabase() {
        MongoClientSettings settings = defaultSettings();
        return MongoClients.create(settings).getDatabase("collar-testing");
    }

    private static MongoClientSettings defaultSettings() {
        return MongoClientSettings.builder()
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY)
                .uuidRepresentation(UuidRepresentation.STANDARD).build();
    }

    private Mongo() {}
}
