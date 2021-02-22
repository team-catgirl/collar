package team.catgirl.collar.server.security.signal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.Binary;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SessionStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class ServerSessionStore implements SessionStore {

    private static final String NAME = "name";
    private static final String DEVICE_ID = "deviceId";
    private static final String RECORD = "record";

    private final MongoCollection<Document> docs;

    public ServerSessionStore(MongoDatabase db) {
        this.docs = db.getCollection("signal_session_store");
        Map<String, Object> index = new HashMap<>();
        index.put(NAME, 1);
        index.put(DEVICE_ID, 1);
        this.docs.createIndex(new Document(index), new IndexOptions().unique(true));
    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        MongoCursor<Document> cursor = docs.find(and(eq(NAME, address.getName()), eq(DEVICE_ID, address.getDeviceId()))).iterator();
        if (cursor.hasNext()) {
            Document doc = cursor.next();
            try {
                return new SessionRecord(doc.get(RECORD, Binary.class).getData());
            } catch (IOException e) {
                throw new IllegalStateException("could not load session record " + address);
            }
        } else {
            return new SessionRecord();
        }
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        return StreamSupport.stream(docs.find(eq(NAME, name)).map(document -> document.getInteger(DEVICE_ID)).spliterator(), false).collect(Collectors.toList());
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        Map<String, Object> state = new HashMap<>();
        state.put(NAME, address.getName());
        state.put(DEVICE_ID, address.getDeviceId());
        state.put(RECORD, record.serialize());
        UpdateResult result = docs.replaceOne(and(eq(NAME, address.getName()), eq(DEVICE_ID, address.getDeviceId())), new Document(state), new ReplaceOptions().upsert(true));
        if (!result.wasAcknowledged() && result.getModifiedCount() != 1) {
            throw new IllegalStateException("did not save session for " + address);
        }
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        return docs.find(and(eq(NAME, address.getName()), eq(DEVICE_ID, address.getDeviceId()))).iterator().hasNext();
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        docs.deleteMany(and(eq(NAME, address.getName()), eq(DEVICE_ID, address.getDeviceId())));
    }

    @Override
    public void deleteAllSessions(String name) {
        docs.deleteMany(eq(NAME, name));
    }
}
