package team.catgirl.collar.security.signal;

import org.junit.Assert;
import org.junit.Test;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.impl.InMemorySignalProtocolStore;
import org.whispersystems.libsignal.util.KeyHelper;

public class PreKeysTest {
    @Test
    public void serialization() throws Exception {
        IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
        SignalProtocolAddress address = new SignalProtocolAddress("test", 1);
        PreKeyBundle bundle = PreKeys.generate(address, new InMemorySignalProtocolStore(identityKeyPair, 1));
        byte[] bytes = PreKeys.preKeyBundleToBytes(bundle);
        PreKeyBundle newBundle = PreKeys.preKeyBundleFromBytes(bytes);
        byte[] newBytes = PreKeys.preKeyBundleToBytes(bundle);

        Assert.assertArrayEquals(bytes, newBytes);

        Assert.assertEquals(bundle.getDeviceId(), newBundle.getDeviceId());
        Assert.assertEquals(bundle.getRegistrationId(), newBundle.getRegistrationId());
        Assert.assertEquals(bundle.getPreKeyId(), newBundle.getPreKeyId());
        Assert.assertEquals(bundle.getPreKey(), newBundle.getPreKey());
        Assert.assertEquals(bundle.getSignedPreKey(), newBundle.getSignedPreKey());
        Assert.assertEquals(bundle.getSignedPreKeyId(), newBundle.getSignedPreKeyId());
        Assert.assertArrayEquals(bundle.getSignedPreKeySignature(), newBundle.getSignedPreKeySignature());
        Assert.assertEquals(bundle.getIdentityKey(), newBundle.getIdentityKey());
    }
}
