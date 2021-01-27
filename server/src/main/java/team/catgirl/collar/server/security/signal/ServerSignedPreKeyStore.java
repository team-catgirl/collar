package team.catgirl.collar.server.security.signal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;

public class ServerSignedPreKeyStore implements SignedPreKeyStore {
    private final MongoCollection<Document> docs;

    public ServerSignedPreKeyStore(MongoDatabase db) {
        this.docs = db.getCollection("signal_signed_prekey_store");
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        MongoCursor<Document> cursor = docs.find(eq("id", signedPreKeyId)).iterator();
        if (cursor.hasNext()) {
            Document doc = cursor.next();
            try {
                return new SignedPreKeyRecord(doc.get("record", Binary.class).getData());
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + signedPreKeyId);
            }
        } else {
            throw new InvalidKeyIdException("could not find key " + signedPreKeyId);
        }
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        return StreamSupport.stream(docs.find().map(document -> {
            try {
                return new SignedPreKeyRecord(document.get("record", byte[].class));
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + document.getInteger("id"));
            }
        }).spliterator(), false).collect(Collectors.toList());
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
        Map<String, Object> state = new HashMap<>();
        state.put("id", signedPreKeyId);
        state.put("record", new Binary(record.serialize()));
        docs.replaceOne(eq("id"), new Document(state));
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        return docs.find(eq("id", signedPreKeyId)).iterator().hasNext();
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        docs.deleteMany(eq("id", signedPreKeyId));
    }
}
