package team.catgirl.collar.server.security.signal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.util.KeyHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class ServerIdentityKeyStore implements IdentityKeyStore {

    private final IdentityKeyPair identityKeyPair;
    private final int registrationId;
    private final MongoCollection<Document> docs;
    private final UUID serverId;

    public ServerIdentityKeyStore(MongoDatabase db) {
        MongoCollection<Document> serverIdentity = db.getCollection("signal_server_identity");
        docs = db.getCollection("signal_key_store");
        MongoCursor<Document> serverIdentityCursor = serverIdentity.find().iterator();
        if (!serverIdentityCursor.hasNext()) {
            identityKeyPair = KeyHelper.generateIdentityKeyPair();
            registrationId = KeyHelper.generateRegistrationId(false);
            serverId = UUID.randomUUID();
            Map<String, Object> state = new HashMap<>();
            state.put("registrationId", registrationId);
            state.put("serverId", serverId);
            state.put("identityKeyPair", new Binary(identityKeyPair.serialize()));
            serverIdentity.insertOne(new Document(state));
        } else {
            Document document = serverIdentityCursor.next();
            this.registrationId = document.getInteger("registrationId");
            this.serverId = document.get("serverId", UUID.class);
            try {
                identityKeyPair = new IdentityKeyPair(document.get("identityKeyPair", Binary.class).getData());
            } catch (InvalidKeyException e) {
                throw  new IllegalStateException("could not load identity key pair", e);
            }
        }
    }

    public UUID getServerId() {
        return serverId;
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        return identityKeyPair;
    }

    @Override
    public int getLocalRegistrationId() {
        return registrationId;
    }

    @Override
    public void saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        docs.replaceOne(and(eq("name", address.getName()), eq("deviceId", address.getDeviceId())), map(address, identityKey));
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        return docs.find(and(eq("name", address.getName()), eq("deviceId", address.getDeviceId()), eq("fingerprint", identityKey.getFingerprint()))).iterator().hasNext();
    }

    private static Document map(SignalProtocolAddress address, IdentityKey identityKey) {
        Map<String, Object> state = new HashMap<>();
        state.put("name", address.getName());
        state.put("deviceId", address.getDeviceId());
        state.put("identityKey", new Binary(identityKey.serialize()));
        state.put("fingerprint", identityKey.getFingerprint());
        return new Document(state);
    }
}
