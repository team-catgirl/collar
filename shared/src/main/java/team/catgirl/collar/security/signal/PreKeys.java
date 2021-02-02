package team.catgirl.collar.security.signal;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.state.*;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.Medium;
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.utils.Utils;

import java.io.*;
import java.util.List;

public final class PreKeys {

    public static PreKeyBundle generate(SignalProtocolAddress address, SignalProtocolStore store) {
        ECKeyPair signedPreKey = Curve.generateKeyPair();
        int signedPreKeyId = Utils.secureRandom().nextInt(Medium.MAX_VALUE);
        ECKeyPair unsignedPreKey = Curve.generateKeyPair();
        int unsignedPreKeyId = Utils.secureRandom().nextInt(Medium.MAX_VALUE);
        byte[] signature;
        try {
            signature = Curve.calculateSignature(store.getIdentityKeyPair().getPrivateKey(), signedPreKey.getPublicKey().serialize());
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("invalid key");
        }
        PreKeyBundle preKeyBundle = new PreKeyBundle(
                store.getLocalRegistrationId(),
                address.getDeviceId(),
                unsignedPreKeyId,
                unsignedPreKey.getPublicKey(),
                signedPreKeyId,
                signedPreKey.getPublicKey(),
                signature,
                store.getIdentityKeyPair().getPublicKey());
        store.storeSignedPreKey(signedPreKeyId, new SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), signedPreKey, signature));
        store.storePreKey(unsignedPreKeyId, new PreKeyRecord(unsignedPreKeyId, unsignedPreKey));
        store.saveIdentity(address, store.getIdentityKeyPair().getPublicKey());
        return preKeyBundle;
    }

    public static PreKeyBundle generate(Identity caller, SignalProtocolStore store) {
        return generate(new SignalProtocolAddress(caller.id().toString(), caller.deviceId()), store);
    }

    public static byte[] preKeyBundleToBytes(PreKeyBundle bundle) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream os = new ObjectOutputStream(bos)) {
                os.writeInt(bundle.getRegistrationId());
                os.writeInt(bundle.getDeviceId());
                os.writeInt(bundle.getPreKeyId());
                writeBytes(os, bundle.getPreKey().serialize());
                os.writeInt(bundle.getSignedPreKeyId());
                writeBytes(os, bundle.getSignedPreKey().serialize());
                writeBytes(os, bundle.getSignedPreKeySignature());
                byte[] identityKey = bundle.getIdentityKey().serialize();
                writeBytes(os, identityKey);
            }
            return bos.toByteArray();
        }
    }

    public static PreKeyBundle preKeyBundleFromBytes(byte[] bytes) throws IOException {
        try (ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            int registrationId = is.readInt();
            int deviceId = is.readInt();
            int preKeyId = is.readInt();
            byte[] preKeyBytes = readBytes(is);
            int signedPreKeyId = is.readInt();
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

    public static void generate(IdentityKeyStore identityKeyStore, PreKeyStore preKeyStore, SignedPreKeyStore signedPreKeyStore) {
        IdentityKeyPair identityKeyPair = identityKeyStore.getIdentityKeyPair();
        List<PreKeyRecord> preKeys = KeyHelper.generatePreKeys(1, 500);
        SignedPreKeyRecord signedPreKey;
        try {
            signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, Utils.secureRandom().nextInt(Medium.MAX_VALUE));
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("problem generating signed preKey", e);
        }
        preKeys.forEach(preKeyRecord -> preKeyStore.storePreKey(preKeyRecord.getId(), preKeyRecord));
        signedPreKeyStore.storeSignedPreKey(signedPreKey.getId(), signedPreKey);
    }

    private static void writeBytes(ObjectOutputStream os, byte[] bytes) throws IOException {
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
}
