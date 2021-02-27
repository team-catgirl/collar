package team.catgirl.collar.client.security;

import org.whispersystems.libsignal.groups.GroupCipher;
import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.groups.state.SenderKeyStore;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.security.signal.AbstractCipher;

public class ClientCipher extends AbstractCipher {

    private final PrivateIdentity privateIdentity;
    private final SenderKeyStore senderKeyStore;
    private final ClientIdentity clientIdentity;

    public ClientCipher(PrivateIdentity privateIdentity, SignalProtocolStore signalProtocolStore, SenderKeyStore senderKeyStore, ClientIdentity clientIdentity) {
        super(signalProtocolStore);
        this.privateIdentity = privateIdentity;
        this.senderKeyStore = senderKeyStore;
        this.clientIdentity = clientIdentity;
    }

    @Override
    public byte[] crypt(Identity sender, Group recipient, byte[] bytes) {
        if (senderKeyStore == null) {
            throw new IllegalStateException("server cannot crypt group messages");
        }
        GroupCipher cipher = new GroupCipher(senderKeyStore, senderKeyNameFrom(recipient, sender));
        try {
            return cipher.encrypt(bytes);
        } catch (Throwable e) {
            throw new IllegalStateException(clientIdentity + " encountered a problem encrypting group message to group " + recipient.id, e);
        }
    }

    @Override
    public byte[] decrypt(Identity sender, Group group, byte[] bytes) {
        if (senderKeyStore == null) {
            throw new IllegalStateException("server cannot decrypt group messages");
        }
        GroupCipher cipher = new GroupCipher(senderKeyStore, senderKeyNameFrom(group, sender));
        try {
            return cipher.decrypt(bytes);
        } catch (Throwable e) {
            throw new IllegalStateException(clientIdentity + " encountered a problem decrypting group message from " + sender, e);
        }
    }

    private static SenderKeyName senderKeyNameFrom(Group group, Identity identity) {
        return new SenderKeyName(group.id.toString(), signalProtocolAddressFrom(identity));
    }

    @Override
    public byte[] crypt(byte[] bytes) {
        return privateIdentity.encrypt(bytes);
    }

    @Override
    public byte[] decrypt(byte[] bytes) {
        return privateIdentity.decrypt(bytes);
    }
}
