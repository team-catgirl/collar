package team.catgirl.collar.security.signal;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.state.PreKeyBundle;

import java.io.*;

public class PreKeyBundles {
    public static byte[] serialize(PreKeyBundle bundle) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream os = new ObjectOutputStream(bos)) {
                os.write(bundle.getRegistrationId());
                os.write(bundle.getDeviceId());
                os.write(bundle.getPreKeyId());
                saveBytes(os, bundle.getPreKey().serialize());
                os.write(bundle.getSignedPreKeyId());
                saveBytes(os, bundle.getSignedPreKey().serialize());
                saveBytes(os, bundle.getSignedPreKeySignature());
                byte[] identityKey = bundle.getIdentityKey().serialize();
                saveBytes(os, identityKey);
            }
            return bos.toByteArray();
        }
    }

    public static PreKeyBundle deserialize(byte[] bytes) throws IOException {
        try (ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            int registrationId = is.read();
            int deviceId = is.read();
            int preKeyId = is.read();
            byte[] preKeyBytes = readBytes(is);
            int signedPreKeyId = is.read();
            byte[] signedPreKeyBytes = readBytes(is);
            byte[] signedPreKeySignatureBytes = readBytes(is);
            byte[] identityKeyBytes = readBytes(is);
            IdentityKey identityKey = new IdentityKey(identityKeyBytes, 0);
            return new PreKeyBundle(
                    registrationId,
                    deviceId,
                    preKeyId,
                    Curve.decodePoint(preKeyBytes, 0),
                    signedPreKeyId,
                    Curve.decodePoint(signedPreKeyBytes, 0),
                    signedPreKeySignatureBytes,
                    identityKey);
        } catch (InvalidKeyException e) {
            throw new IOException(e);
        }
    }

    private static void saveBytes(ObjectOutputStream os, byte[] bytes) throws IOException {
        os.write(bytes.length);
        for (byte b : bytes) {
            os.write(b);
        }
    }

    public static byte[] readBytes(ObjectInputStream is) throws IOException {
        int length = is.read();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = is.readByte();
        }
        return bytes;
    }

    private PreKeyBundles() {}
}
