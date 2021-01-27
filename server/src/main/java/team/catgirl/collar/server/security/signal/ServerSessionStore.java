package team.catgirl.collar.server.security.signal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
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

    private final MongoCollection<Document> docs;

    public ServerSessionStore(MongoDatabase db) {
        this.docs = db.getCollection("signal_session_store");
    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        MongoCursor<Document> cursor = docs.find(and(eq("name", address.getName()), eq("deviceId", address.getDeviceId()))).iterator();
        if (cursor.hasNext()) {
            Document doc = cursor.next();
            try {
                return new SessionRecord(doc.get("record", byte[].class));
            } catch (IOException e) {
                throw new IllegalStateException("could not load session record " + address);
            }
        } else {
            return new SessionRecord();
        }
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        return StreamSupport.stream(docs.find(eq("name", name)).map(document -> document.getInteger("deviceId")).spliterator(), false).collect(Collectors.toList());
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        Map<String, Object> state = new HashMap<>();
        state.put("name", address.getName());
        state.put("deviceId", address.getDeviceId());
        state.put("record", record.serialize());
        docs.replaceOne(and(eq("name", address.getName()), eq("deviceId", address.getDeviceId())), new Document(state));
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        return docs.find(and(eq("name", address.getName()), eq("deviceId", address.getDeviceId()))).iterator().hasNext();
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        docs.deleteMany(and(eq("name", address.getName()), eq("deviceId", address.getDeviceId())));
    }

    @Override
    public void deleteAllSessions(String name) {
        docs.deleteMany(eq("name", name));
    }
}
