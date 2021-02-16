package team.catgirl.collar.security.signal;

import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.ratchet.BobSignalProtocolParameters;
import org.whispersystems.libsignal.ratchet.RatchetingSession;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.util.guava.Optional;
import team.catgirl.collar.protocol.PacketIO;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.Identity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
            } catch (Throwable e) {
                throw new IllegalStateException("Message crypting failed. Recipient " + recipient, e);
            }
        } catch (UntrustedIdentityException e) {
            // TODO: if we get here, then we need to signal back to client with IsUntrusted
            throw new IllegalStateException(e);
        }
    }

    @Override
    public byte[] decrypt(Identity sender, byte[] bytes) {
        SignalProtocolAddress remoteAddress = signalProtocolAddressFrom(sender);
        SessionCipher sessionCipher = new SessionCipher(store, remoteAddress);
        try {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                 ObjectInputStream objectStream = new ObjectInputStream(inputStream)) {
                int type = objectStream.readInt();
                byte[] serialized = PacketIO.toByteArray(objectStream);
                switch (type) {
                    case 0:
                        return sessionCipher.decrypt(new SignalMessage(serialized));
                    case 1:
                        SessionRecord sessionRecord = store.loadSession(remoteAddress);
                        PreKeySignalMessage message = new PreKeySignalMessage(serialized);
                        ECKeyPair ourSignedPreKey = store.loadSignedPreKey(message.getSignedPreKeyId()).getKeyPair();
                        BobSignalProtocolParameters.Builder parameters = BobSignalProtocolParameters.newBuilder();
                        parameters.setTheirBaseKey(message.getBaseKey())
                                .setTheirIdentityKey(message.getIdentityKey())
                                .setOurIdentityKey(store.getIdentityKeyPair())
                                .setOurSignedPreKey(ourSignedPreKey)
                                .setOurRatchetKey(ourSignedPreKey);
                        if (message.getPreKeyId().isPresent() && store.containsPreKey(message.getPreKeyId().get())) {
                            PreKeyRecord preKeyRecord = store.loadPreKey(message.getPreKeyId().get());
                            parameters.setOurOneTimePreKey(Optional.of(preKeyRecord.getKeyPair()));
                        } else {
                            parameters.setOurOneTimePreKey(Optional.absent());
                        }
                        parameters.setOurOneTimePreKey(Optional.absent());
                        if (!sessionRecord.isFresh()) sessionRecord.archiveCurrentState();
                        RatchetingSession.initializeSession(sessionRecord.getSessionState(), parameters.create());
                        sessionRecord.getSessionState().setLocalRegistrationId(store.getLocalRegistrationId());
                        sessionRecord.getSessionState().setRemoteRegistrationId(message.getRegistrationId());
                        sessionRecord.getSessionState().setAliceBaseKey(message.getBaseKey().serialize());
                        return sessionCipher.decrypt(message);
                    default:
                        throw new IllegalStateException("unknown message type '" + type + "'");
                }
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Problem decrypting packet from " + sender, e);
        }
    }

    private static SignalProtocolAddress signalProtocolAddressFrom(Identity identity) {
        return new SignalProtocolAddress(identity.id().toString(), identity.deviceId());
    }
}
