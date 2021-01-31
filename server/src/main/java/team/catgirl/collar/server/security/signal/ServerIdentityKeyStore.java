package team.catgirl.collar.server.security.signal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
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
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class ServerIdentityKeyStore implements IdentityKeyStore {

    // Key store fields
    private static final String NAME = "name";
    private static final String DEVICE_ID = "deviceId";
    private static final String IDENTITY_KEY = "identityKey";

    // Server identity fields
    private static final String FINGERPRINT = "fingerprint";
    private static final String REGISTRATION_ID = "registrationId";
    private static final String SERVER_ID = "serverId";
    private static final String IDENTITY_KEY_PAIR = "identityKeyPair";

    private final IdentityKeyPair identityKeyPair;
    private final int registrationId;
    private final MongoCollection<Document> docs;
    private final UUID serverId;

    public ServerIdentityKeyStore(MongoDatabase db, Consumer<ServerIdentityKeyStore> onInstall) {
        docs = db.getCollection("signal_key_store");
        Map<String, Object> index = new HashMap<>();
        index.put(NAME, 1);
        index.put(DEVICE_ID, 1);
        index.put(FINGERPRINT, 1);
        this.docs.createIndex(new Document(index), new IndexOptions().unique(true));

        // Create or load server identity
        MongoCollection<Document> serverIdentity = db.getCollection("signal_server_identity");
        MongoCursor<Document> serverIdentityCursor = serverIdentity.find().iterator();
        if (!serverIdentityCursor.hasNext()) {
            identityKeyPair = KeyHelper.generateIdentityKeyPair();
            registrationId = KeyHelper.generateRegistrationId(false);
            serverId = UUID.randomUUID();
            Map<String, Object> state = new HashMap<>();
            state.put(REGISTRATION_ID, registrationId);
            state.put(SERVER_ID, serverId);
            state.put(IDENTITY_KEY_PAIR, new Binary(identityKeyPair.serialize()));
            serverIdentity.insertOne(new Document(state));
            onInstall.accept(this);
        } else {
            Document document = serverIdentityCursor.next();
            this.registrationId = document.getInteger(REGISTRATION_ID);
            this.serverId = document.get(SERVER_ID, UUID.class);
            try {
                identityKeyPair = new IdentityKeyPair(document.get(IDENTITY_KEY_PAIR, Binary.class).getData());
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
        if (isTrustedIdentity(address, identityKey)) {
            docs.replaceOne(and(eq(NAME, address.getName()), eq(DEVICE_ID, address.getDeviceId()), eq(FINGERPRINT, identityKey.getFingerprint())), map(address, identityKey));
        } else {
            docs.insertOne(map(address, identityKey));
        }
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        return docs.find(and(eq(NAME, address.getName()), eq(DEVICE_ID, address.getDeviceId()), eq(FINGERPRINT, identityKey.getFingerprint()))).iterator().hasNext();
    }

    public String findNameBy(IdentityKey identityKey, int deviceId) {
        return docs.find(and(eq(FINGERPRINT, identityKey.getFingerprint()), eq(DEVICE_ID, deviceId))).map(document -> document.getString(NAME)).first();
    }

    private static Document map(SignalProtocolAddress address, IdentityKey identityKey) {
        Map<String, Object> state = new HashMap<>();
        state.put(NAME, address.getName());
        state.put(DEVICE_ID, address.getDeviceId());
        state.put(IDENTITY_KEY, new Binary(identityKey.serialize()));
        state.put(FINGERPRINT, identityKey.getFingerprint());
        return new Document(state);
    }
}
