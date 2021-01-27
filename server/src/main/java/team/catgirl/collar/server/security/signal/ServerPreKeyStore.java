package team.catgirl.collar.server.security.signal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
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
    private final MongoCollection<Document> docs;

    public ServerPreKeyStore(MongoDatabase db) {
        this.docs = db.getCollection("signal_prekey_store");
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        MongoCursor<Document> cursor = docs.find(eq("preKeyId", preKeyId)).iterator();
        if (cursor.hasNext()) {
            Document next = cursor.next();
            try {
                return new PreKeyRecord(next.get("record", Binary.class).getData());
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
        state.put("preKeyId", preKeyId);
        state.put("record", new Binary(record.serialize()));
        docs.replaceOne(eq("preKeyId", preKeyId), new Document(state));
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        return docs.find(eq("preKeyId", preKeyId)).iterator().hasNext();
    }

    @Override
    public void removePreKey(int preKeyId) {
        docs.deleteOne(eq("preKeyId", preKeyId));
    }
}
