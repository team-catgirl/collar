package team.catgirl.collar.client.security.signal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.util.List;

public class ClientSignalProtocolStore implements SignalProtocolStore {
    private final ClientIdentityKeyStore identityKeyStore;
    private final ClientPreKeyStore clientPreKeyStore;
    private final ClientSessionStore clientSessionStore;
    private final ClientSignedPreKeyStore clientSignedPreKeyStore;

    public ClientSignalProtocolStore(ClientIdentityKeyStore identityKeyStore, ClientPreKeyStore clientPreKeyStore, ClientSessionStore clientSessionStore, ClientSignedPreKeyStore clientSignedPreKeyStore) {
        this.identityKeyStore = identityKeyStore;
        this.clientPreKeyStore = clientPreKeyStore;
        this.clientSessionStore = clientSessionStore;
        this.clientSignedPreKeyStore = clientSignedPreKeyStore;
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        return identityKeyStore.getIdentityKeyPair();
    }

    @Override
    public int getLocalRegistrationId() {
        return identityKeyStore.getLocalRegistrationId();
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {
        return identityKeyStore.isTrustedIdentity(address, identityKey, direction);
    }

    @Override
    public IdentityKey getIdentity(SignalProtocolAddress address) {
        return identityKeyStore.getIdentity(address);
    }

    @Override
    public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        return identityKeyStore.saveIdentity(address, identityKey);
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        return clientPreKeyStore.loadPreKey(preKeyId);
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        clientPreKeyStore.storePreKey(preKeyId, record);
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        return clientPreKeyStore.containsPreKey(preKeyId);
    }

    @Override
    public void removePreKey(int preKeyId) {
        clientPreKeyStore.removePreKey(preKeyId);
    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        return clientSessionStore.loadSession(address);
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        return clientSessionStore.getSubDeviceSessions(name);
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        clientSessionStore.storeSession(address, record);
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        return clientSessionStore.containsSession(address);
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        clientSessionStore.deleteSession(address);
    }

    @Override
    public void deleteAllSessions(String name) {
        clientSessionStore.deleteAllSessions(name);
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        return clientSignedPreKeyStore.loadSignedPreKey(signedPreKeyId);
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        return clientSignedPreKeyStore.loadSignedPreKeys();
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
        clientSignedPreKeyStore.storeSignedPreKey(signedPreKeyId, record);
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        return clientSignedPreKeyStore.containsSignedPreKey(signedPreKeyId);
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        clientSignedPreKeyStore.removeSignedPreKey(signedPreKeyId);
    }

    public void delete() throws IOException {
        this.identityKeyStore.delete();
        this.clientPreKeyStore.delete();
        this.clientSessionStore.delete();
        this.clientSignedPreKeyStore.delete();
    }

    public static ClientSignalProtocolStore from(HomeDirectory home) throws IOException {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addKeyDeserializer(StateKey.class, new KeyDeserializer() {
            @Override
            public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
                String name = key.substring(0, key.lastIndexOf(":"));
                String id = key.substring(key.lastIndexOf(":") + 1);
                return new StateKey(name, Integer.parseInt(id));
            }
        });
        simpleModule.addKeySerializer(StateKey.class, new JsonSerializer<StateKey>() {
            @Override
            public void serialize(StateKey value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeFieldName(value.name + ":" + value.deviceId);
            }
        });
        ObjectMapper mapper = Utils.jsonMapper().registerModule(simpleModule);
        return new ClientSignalProtocolStore(
                ClientIdentityKeyStore.from(home, mapper),
                ClientPreKeyStore.from(home, mapper),
                ClientSessionStore.from(home, mapper),
                ClientSignedPreKeyStore.from(home, mapper)
        );
    }
}
