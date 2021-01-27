package team.catgirl.collar.security.signal;

import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.ServerIdentity;

import java.io.IOException;

public class SignalCypher implements Cypher {

    private final SignalProtocolStore store;

    public SignalCypher(SignalProtocolStore store) {
        this.store = store;
    }

    @Override
    public byte[] crypt(ServerIdentity serverIdentity, byte[] bytes) {
        SessionBuilder sessionBuilder = new SessionBuilder(store, signalProtocolAddressFrom(serverIdentity));
        try {
            sessionBuilder.process(PreKeyBundles.deserialize(serverIdentity.preKeyBundle));
        } catch (InvalidKeyException | UntrustedIdentityException | IOException e) {
            throw new IllegalStateException(e);
        }
        SessionCipher sessionCipher = new SessionCipher(store, signalProtocolAddressFrom(serverIdentity));
        return sessionCipher.encrypt(bytes).serialize();
    }

    @Override
    public byte[] decrypt(ServerIdentity serverIdentity, byte[] bytes) {
        SessionBuilder sessionBuilder = new SessionBuilder(store, signalProtocolAddressFrom(serverIdentity));
        try {
            sessionBuilder.process(PreKeyBundles.deserialize(serverIdentity.preKeyBundle));
        } catch (InvalidKeyException | UntrustedIdentityException | IOException e) {
            throw new IllegalStateException(e);
        }
        SessionCipher sessionCipher = new SessionCipher(store, signalProtocolAddressFrom(serverIdentity));
        try {
            return sessionCipher.decrypt(new PreKeySignalMessage(bytes));
        } catch (DuplicateMessageException | LegacyMessageException | InvalidMessageException | InvalidKeyIdException | InvalidKeyException | UntrustedIdentityException | InvalidVersionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SignalProtocolAddress signalProtocolAddressFrom(ServerIdentity serverIdentity) {
        return new SignalProtocolAddress(serverIdentity.serverId.toString(), 1);
    }
}
