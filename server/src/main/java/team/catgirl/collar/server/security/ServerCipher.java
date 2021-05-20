package team.catgirl.collar.server.security;

import org.whispersystems.libsignal.state.SignalProtocolStore;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.security.cipher.AbstractCipher;
import team.catgirl.collar.security.cipher.CipherException;

public final class ServerCipher extends AbstractCipher {
    public ServerCipher(SignalProtocolStore signalProtocolStore) {
        super(signalProtocolStore);
    }

    @Override
    public byte[] crypt(byte[] bytes) throws CipherException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public byte[] decrypt(byte[] bytes) throws CipherException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public byte[] crypt(Identity sender, Group recipient, byte[] bytes) throws CipherException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public byte[] decrypt(Identity sender, Group group, byte[] bytes) throws CipherException {
        throw new IllegalStateException("not implemented");
    }
}
