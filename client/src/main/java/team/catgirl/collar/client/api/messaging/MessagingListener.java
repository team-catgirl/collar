package team.catgirl.collar.client.api.messaging;

import team.catgirl.collar.api.messaging.Message;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.ApiListener;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public interface MessagingListener extends ApiListener {
    /**
     * Fired when a private message was sent to another player via collar
     * @param collar client
     * @param messagingApi api
     * @param message the message
     */
    default void onPrivateMessageSent(Collar collar, MessagingApi messagingApi, Message message) {};

    /**
     * Fired when a private message was attempted with another player but there was not sufficent trust to deliver
     * e.g. player was using a non-collar enabled client
     * In this case, you should warn the user and send the Minecraft server protocol message
     * @param collar client
     * @param messagingApi api
     * @param message the message
     */
    default void onPrivateMessageRecipientIsUntrusted(Collar collar, MessagingApi messagingApi, Message message) {};

    /**
     * Fired when a private message was received from another collar player
     * @param collar client
     * @param messagingApi api
     * @param sender who sent it
     * @param message the message
     */
    default void onPrivateMessageReceived(Collar collar, MessagingApi messagingApi, MinecraftPlayer sender, Message message) {};
}
