package team.catgirl.collar.security.signal;

import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import team.catgirl.collar.protocol.PacketIO;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.Identity;

import java.io.*;

public class SignalCypher implements Cypher {

    private final SignalProtocolStore store;

    public SignalCypher(SignalProtocolStore store) {
        this.store = store;
    }

    @Override
    public byte[] crypt(Identity recipient, byte[] bytes) {
        SessionCipher sessionCipher = new SessionCipher(store, signalProtocolAddressFrom(recipient));
        try {
            CiphertextMessage message = sessionCipher.encrypt(bytes);
            int type;
            if (message instanceof SignalMessage) {
                type = 0;
            } else if (message instanceof PreKeySignalMessage) {
                type = 1;
            } else {
                throw new IllegalStateException("unknown message type " + message.getClass().getName());
            }
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                try (ObjectOutputStream objectStream = new ObjectOutputStream(outputStream)) {
                    objectStream.writeInt(type);
                    objectStream.write(message.serialize());
                }
                return outputStream.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } catch (UntrustedIdentityException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public byte[] decrypt(Identity sender, byte[] bytes) {
        SessionCipher sessionCipher = new SessionCipher(store, signalProtocolAddressFrom(sender));
        try {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                try (ObjectInputStream objectStream = new ObjectInputStream(inputStream)) {
                    int type = objectStream.readInt();
                    byte[] serialized = PacketIO.toByteArray(objectStream);
                    switch (type) {
                        case 0:
                            return sessionCipher.decrypt(new SignalMessage(serialized));
                        case 1:
                            return sessionCipher.decrypt(new PreKeySignalMessage(serialized));
                        default:
                            throw new IllegalStateException("unknown message type");
                    }
                }
            }
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static SignalProtocolAddress signalProtocolAddressFrom(Identity identity) {
        return new SignalProtocolAddress(identity.id().toString(), identity.deviceId());
    }
}
