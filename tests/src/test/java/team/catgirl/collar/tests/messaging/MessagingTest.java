package team.catgirl.collar.tests.messaging;

import org.junit.Test;
import team.catgirl.collar.api.messaging.Message;
import team.catgirl.collar.api.messaging.TextMessage;
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
        bobPlayer.collar.messaging().subscribe(bobMessageListener);
        alicePlayer.collar.messaging().sendPrivateMessage(bobPlayer.collar.player(), new TextMessage("UwU"));
        waitForCondition("Alice sent an UwU to Bob", () -> {
            if (!(bobMessageListener.lastMessage instanceof TextMessage)) {
                return false;
            }
            TextMessage message = (TextMessage) bobMessageListener.lastMessage;
            return message.content.equals("UwU");
        });
    }

    @Test
    public void aliceSendsNonCollarPlayerAnUwU() {
        MessagingListenerImpl aliceListener = new MessagingListenerImpl();
        alicePlayer.collar.messaging().subscribe(aliceListener);
        alicePlayer.collar.messaging().sendPrivateMessage(new MinecraftPlayer(UUID.randomUUID(), "hypixel.net"), new TextMessage("UwU"));
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

        @Override
        public void onPrivateMessageReceived(Collar collar, MessagingApi messagingApi, MinecraftPlayer sender, Message message) {
            this.lastMessage = message;
        }

        @Override
        public void onPrivateMessageRecipientIsUntrusted(Collar collar, MessagingApi messagingApi, MinecraftPlayer player, Message message) {
            this.lastUntrustedMessage = message;
        }
    }
}
