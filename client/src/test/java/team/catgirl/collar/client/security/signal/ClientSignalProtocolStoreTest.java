package team.catgirl.collar.client.security.signal;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.groups.GroupSessionBuilder;
import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.security.ClientIdentity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientSignalProtocolStoreTest {
    private ClientSignalProtocolStore store;
    File tempDir;
    UUID playerId;

    @Before
    public void createStore() throws IOException {
        tempDir = Files.createTempDir();
        playerId = UUID.randomUUID();
        store = ClientSignalProtocolStore.from(HomeDirectory.from(tempDir, "localhost"));
    }

    @Test
    public void loadsFromDisk() throws Exception {
        IdentityKeyPair identityKeyPair = store.getIdentityKeyPair();
        int registrationId = store.getLocalRegistrationId();
        store = ClientSignalProtocolStore.from(HomeDirectory.from(tempDir, "localhost"));
        Assert.assertEquals(store.getLocalRegistrationId(), registrationId);
        Assert.assertArrayEquals(store.getIdentityKeyPair().serialize(), identityKeyPair.serialize());
    }

    @Test
    public void signedPreKeyStore() throws Exception {
        IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
        SignedPreKeyRecord signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 0);
        store.storeSignedPreKey(signedPreKey.getId(), signedPreKey);
        SignedPreKeyRecord loaded = store.loadSignedPreKey(signedPreKey.getId());
        Assert.assertArrayEquals(signedPreKey.serialize(), loaded.serialize());
        store.removeSignedPreKey(signedPreKey.getId());
        try {
            store.loadSignedPreKey(signedPreKey.getId());
            Assert.fail("key was present");
        } catch (InvalidKeyIdException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void identityKeyStore() {
        Assert.assertTrue(store.getLocalRegistrationId() > 0);
        Assert.assertNotNull(store.getIdentityKeyPair());
        IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
        SignalProtocolAddress address = new SignalProtocolAddress("testKey", 1);
        Assert.assertFalse(store.isTrustedIdentity(address, identityKeyPair.getPublicKey(), null));
        store.saveIdentity(address, identityKeyPair.getPublicKey());
        Assert.assertTrue(store.isTrustedIdentity(address, identityKeyPair.getPublicKey(), null));
    }

    @Test
    public void sessionStore() {
        SignalProtocolAddress address1 = new SignalProtocolAddress("testKey", 1);
        Assert.assertFalse(store.containsSession(address1));
        SessionRecord sessionRecord = store.loadSession(address1);
        Assert.assertNotNull(sessionRecord);
        Assert.assertFalse(store.containsSession(address1));
        store.storeSession(address1, sessionRecord);
        Assert.assertTrue(store.containsSession(address1));
        store.deleteSession(address1);
        Assert.assertFalse(store.containsSession(address1));
        store.storeSession(address1, sessionRecord);
        Assert.assertTrue(store.containsSession(address1));
        store.deleteAllSessions(address1.getName());

        SignalProtocolAddress address2 = new SignalProtocolAddress("testKey", 2);
        store.storeSession(address1, sessionRecord);
        store.storeSession(address2, store.loadSession(address2));
        List<Integer> testKey = store.getSubDeviceSessions("testKey");
        List<Integer> expected = new ArrayList<>();
        expected.add(1);
        expected.add(2);
        Assert.assertEquals(expected, testKey);
        Assert.assertTrue(store.containsSession(address1));
        Assert.assertTrue(store.containsSession(address2));
        store.deleteAllSessions(address1.getName());
        Assert.assertFalse(store.containsSession(address1));
        Assert.assertFalse(store.containsSession(address2));
    }

    @Test
    public void preKeyStore() throws Exception {
        List<PreKeyRecord> preKeys = KeyHelper.generatePreKeys(0, 500);
        Assert.assertEquals(500, preKeys.size());
        for (PreKeyRecord preKeyRecord : preKeys) {
            Assert.assertFalse(store.containsPreKey(preKeyRecord.getId()));
            try {
                store.loadPreKey(preKeyRecord.getId());
                Assert.fail("Loaded a prekey");
            } catch (InvalidKeyIdException ignored) {}
        }
        preKeys.forEach(preKeyRecord -> store.storePreKey(preKeyRecord.getId(), preKeyRecord));
        for (PreKeyRecord preKeyRecord : preKeys) {
            Assert.assertTrue(store.containsPreKey(preKeyRecord.getId()));
            PreKeyRecord loaded = store.loadPreKey(preKeyRecord.getId());
            Assert.assertArrayEquals(preKeyRecord.serialize(), loaded.serialize());
        }

        for (PreKeyRecord preKeyRecord : preKeys) {
            Assert.assertTrue(store.containsPreKey(preKeyRecord.getId()));
            store.removePreKey(preKeyRecord.getId());
            Assert.assertFalse(store.containsPreKey(preKeyRecord.getId()));
        }
    }

    @Test
    public void senderKeyStore() throws InvalidKeyIdException, InvalidKeyException {
        UUID profile = UUID.randomUUID();
        UUID group = UUID.randomUUID();
        SenderKeyName keyName = new SenderKeyName(group.toString(), new SignalProtocolAddress(profile.toString(), 1));
        GroupSessionBuilder builder = new GroupSessionBuilder(store);
        Assert.assertTrue(store.loadSenderKey(keyName).isEmpty());
        SenderKeyDistributionMessage distributionMessage = builder.create(keyName);

        Assert.assertNotEquals(0, distributionMessage.getChainKey().length);
        Assert.assertNotEquals(0, distributionMessage.getId());
        Assert.assertNotEquals(0, distributionMessage.getSignatureKey().serialize().length);

        Assert.assertNotNull(store.loadSenderKey(keyName));
        Assert.assertFalse(store.loadSenderKey(keyName).isEmpty());
        Assert.assertNotEquals(0, store.loadSenderKey(keyName).getSenderKeyState().getSigningKeyPrivate().serialize().length);
        Assert.assertNotEquals(0, store.loadSenderKey(keyName).getSenderKeyState().getSigningKeyPublic().serialize().length);
        Assert.assertNotEquals(0, store.loadSenderKey(keyName).getSenderKeyState().getSenderChainKey().getSenderMessageKey().getCipherKey().length);
        Assert.assertNotEquals(0, store.loadSenderKey(keyName).getSenderKeyState().getSenderChainKey().getSenderMessageKey().getIv().length);
        Assert.assertNotEquals(0, store.loadSenderKey(keyName).getSenderKeyState().getSenderChainKey().getSenderMessageKey().getSeed().length);

        store.deleteGroupSession(group, new ClientIdentity(profile, null,1));
        Assert.assertTrue(store.loadSenderKey(keyName).isEmpty());

        SenderKeyName keyName2 = new SenderKeyName(group.toString(), new SignalProtocolAddress(profile.toString(), 1));
        builder.create(keyName);
        builder.create(keyName2);
        Assert.assertFalse(store.loadSenderKey(keyName).isEmpty());
        Assert.assertFalse(store.loadSenderKey(keyName2).isEmpty());

        store.deleteAllGroupSessions();
        Assert.assertTrue(store.loadSenderKey(keyName).isEmpty());
        Assert.assertTrue(store.loadSenderKey(keyName2).isEmpty());
    }
}
