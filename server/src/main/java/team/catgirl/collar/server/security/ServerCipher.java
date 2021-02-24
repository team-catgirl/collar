package team.catgirl.collar.server.security;

import org.whispersystems.libsignal.state.SignalProtocolStore;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.security.signal.AbstractCipher;

public final class ServerCipher extends AbstractCipher {
    public ServerCipher(SignalProtocolStore signalProtocolStore) {
        super(signalProtocolStore);
    }

    @Override
    public byte[] crypt(byte[] bytes) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public byte[] decrypt(byte[] bytes) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public byte[] crypt(Identity sender, Group recipient, byte[] bytes) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public byte[] decrypt(Identity sender, Group group, byte[] bytes) {
        throw new IllegalStateException("not implemented");
    }
}
