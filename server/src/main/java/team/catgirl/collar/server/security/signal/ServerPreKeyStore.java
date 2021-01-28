package team.catgirl.collar.server.security.signal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.types.Binary;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

public class ServerPreKeyStore implements PreKeyStore {
    private static final String PRE_KEY_ID = "preKeyId";
    private static final String RECORD = "record";

    private final MongoCollection<Document> docs;

    public ServerPreKeyStore(MongoDatabase db) {
        this.docs = db.getCollection("signal_prekey_store");
        Map<String, Object> index = new HashMap<>();
        index.put(PRE_KEY_ID, 1);
        this.docs.createIndex(new Document(index), new IndexOptions().unique(true));
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        MongoCursor<Document> cursor = docs.find(eq(PRE_KEY_ID, preKeyId)).iterator();
        if (cursor.hasNext()) {
            Document next = cursor.next();
            try {
                return new PreKeyRecord(next.get(RECORD, Binary.class).getData());
            } catch (IOException e) {
                throw new IllegalStateException("could not load key", e);
            }
        } else {
            throw new InvalidKeyIdException("could not load key " + preKeyId);
        }
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        Map<String, Object> state = new HashMap<>();
        state.put(PRE_KEY_ID, preKeyId);
        state.put(RECORD, new Binary(record.serialize()));
        docs.insertOne(new Document(state));
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        return docs.find(eq(PRE_KEY_ID, preKeyId)).iterator().hasNext();
    }

    @Override
    public void removePreKey(int preKeyId) {
        docs.deleteOne(eq(PRE_KEY_ID, preKeyId));
    }
}
