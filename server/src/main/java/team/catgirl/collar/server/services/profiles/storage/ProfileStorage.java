package team.catgirl.collar.server.services.profiles.storage;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.Binary;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class ProfileStorage {
    public static final String FIELD_KEY = "key";
    public static final String FIELD_OWNER = "owner";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_DATA = "data";

    private final MongoCollection<Document> docs;

    public ProfileStorage(MongoDatabase db) {
        this.docs = db.getCollection("profile_storage");
        Map<String, Object> index = Map.of(FIELD_OWNER, 1, FIELD_KEY, 1, FIELD_DATA, 1);
        this.docs.createIndex(new Document(index));
    }

    public void store(UUID owner, UUID key, byte[] data, String type) {
        Map<String, Object> state = Map.of(FIELD_OWNER, owner, FIELD_KEY, key, FIELD_DATA, new Binary(data), FIELD_TYPE, type);
        UpdateResult result = docs.updateOne(and(eq(FIELD_OWNER, owner), eq(FIELD_KEY, key)), new Document("$set", new Document(state)), new UpdateOptions().upsert(true));
        if (!result.wasAcknowledged()) {
            throw new IllegalStateException("could not store data for owner " + owner + " key " + key);
        }
    }

    public void delete(UUID owner, UUID key) {
        DeleteResult result = docs.deleteOne(and(eq(FIELD_OWNER, owner), eq(FIELD_KEY, key)));
        if (!result.wasAcknowledged()) {
            throw new IllegalStateException("could not delete data for owner " + owner + " key " + key);
        }
    }

    public List<Blob> find(UUID owner, String type) {
        MongoCursor<Blob> iterator = docs.find(and(eq(FIELD_OWNER, owner), eq(FIELD_TYPE, type))).map(document -> new Blob(
                document.get(FIELD_OWNER, UUID.class),
                document.get(FIELD_KEY, UUID.class),
                document.get(FIELD_TYPE, String.class),
                document.get(FIELD_DATA, Binary.class).getData()
        )).iterator();
        List<Blob> blobs = new ArrayList<>();
        while (iterator.hasNext()) {
            blobs.add(iterator.next());
        }
        return blobs;
    }

    public static final class Blob {
        public final UUID owner;
        public final UUID key;
        public final String type;
        public final byte[] data;

        public Blob(UUID owner, UUID key, String type, byte[] data) {
            this.owner = owner;
            this.key = key;
            this.type = type;
            this.data = data;
        }
    }
}
