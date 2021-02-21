package team.catgirl.collar.client.api.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.messaging.Message;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.api.identity.IdentityApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.messaging.SendMessageRequest;
import team.catgirl.collar.protocol.messaging.SendMessageResponse;
import team.catgirl.collar.security.cipher.Cipher;
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
                .thenAccept(sender -> {
                    if (sender.isPresent()) {
                        Cipher cipher = identityStore().createCypher();
                        byte[] messageBytes;
                        try {
                            messageBytes = cipher.crypt(sender.get(), Utils.messagePackMapper().writeValueAsBytes(message));
                        } catch (JsonProcessingException e) {
                            throw new IllegalStateException(collar.identity() + " could not process private message from " + sender, e);
                        }
                        this.sender.accept(new SendMessageRequest(collar.identity(), sender.get(), null, messageBytes));
                        fireListener("onPrivateMessageSent", listener -> {
                            listener.onPrivateMessageSent(collar, this, player, message);
                        });
                    } else {
                        LOGGER.log(Level.INFO, collar.identity() + " could not locate identity for " + player + ". The private message was not sent.");
                        fireListener("onPrivateMessageRecipientIsUntrusted", listener -> {
                            listener.onPrivateMessageRecipientIsUntrusted(collar, this, player, message);
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
        LOGGER.log(Level.INFO, identity() + " sending message to group " + group.id);
        Cipher cipher = identityStore().createCypher();
        byte[] messageBytes;
        try {
            messageBytes = cipher.crypt(identity(), group, Utils.messagePackMapper().writeValueAsBytes(message));
        } catch (Throwable e) {
            // If the client cant send a message to the group, something is seriously wrong
            throw new IllegalStateException(collar.identity() + " could not encrypt group message sent to " + group.id, e);
        };
        sender.accept(new SendMessageRequest(collar.identity(), null, group.id, messageBytes));
        LOGGER.log(Level.INFO, identity() + " sent message to group " + group.id);
        fireListener("onGroupMessageSent", listener -> {
            listener.onGroupMessageSent(collar, this, group, message);
        });
    }

    /**
     * Send a message to players nearby
     * @param message to send
     */
    public void sendNearbyMessage(Message message) {
        collar.groups().locationGroups().forEach(group -> sendGroupMessage(group, message));
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof SendMessageResponse) {
            SendMessageResponse response = (SendMessageResponse) resp;
            if (response.group != null && response.sender != null) {
                collar.groups().findGroupById(response.group).ifPresent(group -> {
                    byte[] decryptedBytes = identityStore().createCypher().decrypt(response.sender, group, response.message);
                    Message message;
                    try {
                        message = Utils.messagePackMapper().readValue(decryptedBytes, Message.class);
                    } catch (IOException e) {
                        // We don't throw an exception here in case someone is doing something naughty to disrupt the group and cause the client to exit
                        LOGGER.log(Level.SEVERE, collar.identity() + "could not read group message from group " + group.id, e);
                        message = null;
                    }
                    if (message != null) {
                        Message finalMessage = message;
                        fireListener("onGroupMessageReceived", listener -> {
                            listener.onGroupMessageReceived(collar, this, group, response.player, finalMessage);
                        });
                    }
                });
            } else if (response.sender != null) {
                byte[] decryptedBytes = identityStore().createCypher().decrypt(response.sender, response.message);
                Message message;
                try {
                    message = Utils.messagePackMapper().readValue(decryptedBytes, Message.class);
                } catch (IOException e) {
                    throw new IllegalStateException(collar.identity() + "Could not read private message from " + response.sender, e);
                }
                fireListener("onPrivateMessageReceived", listener -> {
                    listener.onPrivateMessageReceived(collar, this, response.player, message);
                });
            } else {
                LOGGER.log(Level.WARNING, collar.identity() + "could not process message. It was not addressed correctly.");
            }
            return true;
        }
        return false;
    }

    @Override
    public void onStateChanged(Collar.State state) {}
}
