package team.catgirl.collar.server.mongo;


import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public final class Mongo {

    private static MongoDatabase database;

    public static MongoDatabase database() {
        if (database == null) {
            String mongoUrl = System.getenv("MONGODB_URI");
            database = mongoUrl == null ? getDevelopmentDatabase() : database(mongoUrl);
        }
        return database;
    }

    private Mongo() {}

    public static MongoDatabase database(String mongoUrl) {
        ConnectionString uri = new ConnectionString(mongoUrl);
        MongoClient mongoClient = MongoClients.create(uri);
        String db = uri.getDatabase();
        if (db == null) {
            throw new IllegalStateException("MONGODB_URI did not include database name");
        }
        return mongoClient.getDatabase(db);
    }

    private static MongoDatabase getDevelopmentDatabase() {
        return MongoClients.create().getDatabase("backoffice-dev");
    }
}
