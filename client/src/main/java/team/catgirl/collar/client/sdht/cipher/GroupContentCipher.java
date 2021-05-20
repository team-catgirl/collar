package team.catgirl.collar.client.sdht.cipher;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.sdht.Content;
import team.catgirl.collar.sdht.cipher.ContentCipher;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.cipher.CipherException;

import java.util.UUID;
import java.util.function.Supplier;

public final class GroupContentCipher implements ContentCipher {

    private final GroupsApi groupsApi;
    private final Supplier<ClientIdentityStore> identityStoreSupplier;

    public GroupContentCipher(GroupsApi groupsApi, Supplier<ClientIdentityStore> identityStoreSupplier) {
        this.groupsApi = groupsApi;
        this.identityStoreSupplier = identityStoreSupplier;
    }

    @Override
    public byte[] crypt(ClientIdentity identity, UUID namespace, Content content) throws CipherException {
        Group group = groupsApi.findGroupById(namespace).orElseThrow(() -> new IllegalStateException("could not find group " + namespace));
        return identityStoreSupplier.get().createCypher().crypt(identity, group, content.serialize());
    }

    @Override
    public Content decrypt(ClientIdentity identity, UUID namespace, byte[] bytes) throws CipherException {
        Group group = groupsApi.findGroupById(namespace).orElseThrow(() -> new IllegalStateException("could not find group " + namespace));
        byte[] decryptedBytes = identityStoreSupplier.get().createCypher().decrypt(identity, group, bytes);
        return new Content(decryptedBytes);
    }

    @Override
    public boolean accepts(UUID namespace) {
        return groupsApi.findGroupById(namespace).isPresent();
    }
}
