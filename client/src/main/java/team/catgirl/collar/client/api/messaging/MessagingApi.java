package team.catgirl.collar.client.api.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import team.catgirl.collar.api.groups.Group;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagingApi extends AbstractApi<MessagingListener> {

    private static final Logger LOGGER = Logger.getLogger(MessagingApi.class.getName());

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
                            throw new IllegalStateException("Could not process private message", e);
                        }
                        sender.accept(new SendMessageRequest(collar.identity(), identity, null, messageBytes));
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

    /**
     * Sends a message to the specified group
     * @param group to send to
     * @param message to send
     */
    public void sendGroupMessage(Group group, Message message) {
        Cypher cypher = identityStore().createCypher();
        byte[] messageBytes;
        try {
            messageBytes = cypher.crypt(identity(), group, Utils.messagePackMapper().writeValueAsBytes(message));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not process group message", e);
        }
        sender.accept(new SendMessageRequest(collar.identity(), null, group.id, messageBytes));
        fireListener("", listener -> {
            listener.onGroupMessageSent(collar, this, group, message);
        });
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof SendMessageResponse) {
            SendMessageResponse response = (SendMessageResponse) resp;
            if (response.group != null && response.sender != null) {
                collar.groups().all().stream().filter(candidate -> candidate.id.equals(response.group))
                    .findFirst()
                    .ifPresent(group -> {
                        byte[] decryptedBytes = identityStore().createCypher().decrypt(response.sender, group, response.message);
                        Message message;
                        try {
                            message = Utils.messagePackMapper().readValue(decryptedBytes, Message.class);
                        } catch (IOException e) {
                            throw new IllegalStateException("Could not read group message", e);
                        }
                        fireListener("onPrivateMessageReceived", listener -> {
                            listener.onGroupMessageReceived(collar, this, group, response.player, message);
                        });
                    });
            } else if (response.sender != null) {
                byte[] decryptedBytes = identityStore().createCypher().decrypt(response.sender, response.message);
                Message message;
                try {
                    message = Utils.messagePackMapper().readValue(decryptedBytes, Message.class);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read private message", e);
                }
                fireListener("onPrivateMessageReceived", listener -> {
                    listener.onPrivateMessageReceived(collar, this, response.player, message);
                });
            } else {
                LOGGER.log(Level.WARNING, "Message could not be process. It was not addressed correctly.");
            }
            return true;
        }
        return false;
    }

    @Override
    public void onStateChanged(Collar.State state) {}
}
