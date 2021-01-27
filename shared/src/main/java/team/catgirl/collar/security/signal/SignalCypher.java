package team.catgirl.collar.security.signal;

import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.Identity;

public class SignalCypher implements Cypher {

    private final SignalProtocolStore store;

    public SignalCypher(SignalProtocolStore store) {
        this.store = store;
    }

    @Override
    public byte[] crypt(Identity identity, byte[] bytes) {
        SessionCipher sessionCipher = new SessionCipher(store, signalProtocolAddressFrom(identity));
        return sessionCipher.encrypt(bytes).serialize();
    }

    @Override
    public byte[] decrypt(Identity identity, byte[] bytes) {
        SessionCipher sessionCipher = new SessionCipher(store, signalProtocolAddressFrom(identity));
        try {
            return sessionCipher.decrypt(new PreKeySignalMessage(bytes));
        } catch (DuplicateMessageException | LegacyMessageException | InvalidMessageException | InvalidKeyIdException | InvalidKeyException | UntrustedIdentityException | InvalidVersionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SignalProtocolAddress signalProtocolAddressFrom(Identity identity) {
        return new SignalProtocolAddress(identity.id().toString(), 1);
    }
}
