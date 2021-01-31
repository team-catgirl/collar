package team.catgirl.collar.security.signal;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.state.*;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.Medium;
import team.catgirl.collar.utils.Utils;

import java.io.*;
import java.util.List;

public class PreKeys {

    public static PreKeyBundle generate(SignalProtocolStore store, int deviceId) {
        ECKeyPair signedPreKey = Curve.generateKeyPair();
        int signedPreKeyId = Utils.createSecureRandom().nextInt(Medium.MAX_VALUE);
        ECKeyPair unsignedPreKey = Curve.generateKeyPair();
        int unsignedPreKeyId = Utils.createSecureRandom().nextInt(Medium.MAX_VALUE);
        byte[] signature;
        try {
//            Curve.calculateSignature(identityKeyPair.getPrivateKey(), keyPair.getPublicKey().serialize())
            signature = Curve.calculateSignature(store.getIdentityKeyPair().getPrivateKey(), signedPreKey.getPublicKey().serialize());
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("invalid key");
        }
        PreKeyBundle preKeyBundle = new PreKeyBundle(
                store.getLocalRegistrationId(),
                deviceId,
                unsignedPreKeyId,
                unsignedPreKey.getPublicKey(),
                signedPreKeyId,
                signedPreKey.getPublicKey(),
                signature,
                store.getIdentityKeyPair().getPublicKey());

        store.storeSignedPreKey(signedPreKeyId, new SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), signedPreKey, signature));
        store.storePreKey(unsignedPreKeyId, new PreKeyRecord(unsignedPreKeyId, unsignedPreKey));
        return preKeyBundle;
    }

    public static byte[] preKeyBundleToBytes(PreKeyBundle bundle) throws IOException {
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

    public static PreKeyBundle preKeyBundleFromBytes(byte[] bytes) throws IOException {
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

    private static byte[] readBytes(ObjectInputStream is) throws IOException {
        int length = is.read();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = is.readByte();
        }
        return bytes;
    }

    private PreKeys() {}

    public static void generate(IdentityKeyStore identityKeyStore, PreKeyStore preKeyStore, SignedPreKeyStore signedPreKeyStore) {
        IdentityKeyPair identityKeyPair = identityKeyStore.getIdentityKeyPair();
        List<PreKeyRecord> preKeys = KeyHelper.generatePreKeys(0, 500);
        SignedPreKeyRecord signedPreKey;
        try {
            signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, Utils.createSecureRandom().nextInt(Medium.MAX_VALUE));
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("problem generating signed preKey", e);
        }
        preKeys.forEach(preKeyRecord -> preKeyStore.storePreKey(preKeyRecord.getId(), preKeyRecord));
        signedPreKeyStore.storeSignedPreKey(signedPreKey.getId(), signedPreKey);
    }
}
