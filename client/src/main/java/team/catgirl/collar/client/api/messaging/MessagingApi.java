package team.catgirl.collar.client.api.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import team.catgirl.collar.api.messaging.Message;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.api.identity.IdentityApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.messaging.SendMessageRequest;
import team.catgirl.collar.protocol.messaging.SendMessageResponse;
import team.catgirl.collar.security.Cypher;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MessagingApi extends AbstractApi<MessagingListener> {

    public MessagingApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * Sends a private message to another player
     * @param player to send the message to
     * @param message the player
     */
    public void sendPrivateMessage(MinecraftPlayer player, Message message) {
        IdentityApi identityApi = collar.identities();
        identityApi.identify(player.id)
                .thenCompose(identityApi::createTrust)
                .thenAccept(identity -> {
                    if (identity != null) {
                        Cypher cypher = identityStore().createCypher();
                        byte[] messageBytes;
                        try {
                            messageBytes = cypher.crypt(identity, Utils.messagePackMapper().writeValueAsBytes(message));
                        } catch (JsonProcessingException e) {
                            throw new IllegalStateException("Could not process message", e);
                        }
                        sender.accept(new SendMessageRequest(collar.identity(), identity, messageBytes));
                        fireListener("onPrivateMessageSent", listener -> {
                            listener.onPrivateMessageSent(collar, this, message);
                        });
                    } else {
                        fireListener("onPrivateMessageRecipientIsUntrusted", listener -> {
                            listener.onPrivateMessageRecipientIsUntrusted(collar, this, message);
                        });
                    }
                });
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof SendMessageResponse) {
            SendMessageResponse response = (SendMessageResponse) resp;
            fireListener("onPrivateMessageReceived", listener -> {
                byte[] decryptedBytes = identityStore().createCypher().decrypt(response.sender, response.message);
                Message message;
                try {
                    message = Utils.messagePackMapper().readValue(decryptedBytes, Message.class);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read message", e);
                }
                listener.onPrivateMessageReceived(collar, this, response.player, message);
            });
            return true;
        }
        return false;
    }

    @Override
    public void onStateChanged(Collar.State state) {}
}
