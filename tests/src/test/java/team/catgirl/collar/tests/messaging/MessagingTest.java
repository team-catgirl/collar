package team.catgirl.collar.tests.messaging;

import org.junit.Assert;
import org.junit.Test;
import team.catgirl.collar.api.messaging.Message;
import team.catgirl.collar.api.messaging.TextMessage;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.messaging.MessagingApi;
import team.catgirl.collar.client.api.messaging.MessagingListener;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.tests.junit.CollarTest;

import java.util.UUID;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class MessagingTest extends CollarTest {
    @Test
    public void aliceSendsBobAnUwU() {
        MessagingListenerImpl bobMessageListener = new MessagingListenerImpl();
        MessagingListenerImpl aliceListener = new MessagingListenerImpl();
        bobPlayer.collar.messaging().subscribe(bobMessageListener);
        alicePlayer.collar.messaging().subscribe(aliceListener);
        alicePlayer.collar.messaging().sendPrivateMessage(bobPlayer.collar.player().minecraftPlayer, new TextMessage("UwU"));
        waitForCondition("Alice sent an UwU to Bob", () -> {
            if (!(bobMessageListener.lastMessage instanceof TextMessage)) {
                return false;
            }
            TextMessage message = (TextMessage) bobMessageListener.lastMessage;
            return message.content.equals("UwU");
        });
        Assert.assertEquals("UwU", ((TextMessage)aliceListener.lastMessageSent).content);
    }

    @Test
    public void aliceSendsNonCollarPlayerAnUwU() {
        MessagingListenerImpl aliceListener = new MessagingListenerImpl();
        alicePlayer.collar.messaging().subscribe(aliceListener);
        alicePlayer.collar.messaging().sendPrivateMessage(new MinecraftPlayer(UUID.randomUUID(), "hypixel.net", 1), new TextMessage("UwU"));
        waitForCondition("Alice could not send private message", () -> {
            if (!(aliceListener.lastUntrustedMessage instanceof TextMessage)) {
                return false;
            }
            TextMessage message = (TextMessage) aliceListener.lastUntrustedMessage;
            return message.content.equals("UwU");
        });
    }

    private static final class MessagingListenerImpl implements MessagingListener {
        Message lastMessage;
        Message lastUntrustedMessage;
        Message lastMessageSent;

        @Override
        public void onPrivateMessageReceived(Collar collar, MessagingApi messagingApi, Player sender, Message message) {
            this.lastMessage = message;
        }

        @Override
        public void onPrivateMessageRecipientIsUntrusted(Collar collar, MessagingApi messagingApi, MinecraftPlayer player, Message message) {
            this.lastUntrustedMessage = message;
        }

        @Override
        public void onPrivateMessageSent(Collar collar, MessagingApi messagingApi, Player player, Message message) {
            this.lastMessageSent = message;
        }
    }
}
