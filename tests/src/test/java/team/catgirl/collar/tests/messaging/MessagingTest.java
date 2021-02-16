package team.catgirl.collar.tests.messaging;

import org.junit.Test;
import team.catgirl.collar.api.messaging.Message;
import team.catgirl.collar.api.messaging.TextMessage;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.messaging.MessagingApi;
import team.catgirl.collar.client.api.messaging.MessagingListener;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.tests.junit.CollarAssert;
import team.catgirl.collar.tests.junit.CollarTest;

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

    private static final class MessagingListenerImpl implements MessagingListener {
        Message lastMessage;

        @Override
        public void onPrivateMessageReceived(Collar collar, MessagingApi messagingApi, MinecraftPlayer sender, Message message) {
            this.lastMessage = message;
        }
    }
}
