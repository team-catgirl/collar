package team.catgirl.collar.tests.messaging;

import org.junit.Assert;
import org.junit.Test;
import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.api.entities.EntityType;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.messaging.TextMessage;
import team.catgirl.collar.tests.groups.GroupsTest.MessagingListenerImpl;
import team.catgirl.collar.tests.groups.GroupsTest.TestGroupsListener;
import team.catgirl.collar.tests.junit.CollarTest;

import java.util.Set;
import java.util.UUID;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class NearbyMessagingTest extends CollarTest {

    UUID aliceEntityId = UUID.randomUUID();
    UUID bobEntityId = UUID.randomUUID();
    UUID eveEntityId = UUID.randomUUID();

    @Test
    public void sendNearbyMessage() {
        TestGroupsListener aliceGroups = new TestGroupsListener();
        MessagingListenerImpl aliceMessages = new MessagingListenerImpl();
        alicePlayer.collar.messaging().subscribe(aliceMessages);
        alicePlayer.collar.groups().subscribe(aliceGroups);

        MessagingListenerImpl bobMessages = new MessagingListenerImpl();
        TestGroupsListener bobGroups = new TestGroupsListener();
        bobPlayer.collar.messaging().subscribe(bobMessages);
        bobPlayer.collar.groups().subscribe(bobGroups);

        MessagingListenerImpl eveMessages = new MessagingListenerImpl();
        TestGroupsListener eveGroups = new TestGroupsListener();
        evePlayer.collar.messaging().subscribe(eveMessages);
        evePlayer.collar.groups().subscribe(eveGroups);

        waitForCondition("Alice joined nearby group", () -> !alicePlayer.collar.groups().nearbyGroups().isEmpty());
        waitForCondition("Bob joined nearby group", () -> !bobPlayer.collar.groups().nearbyGroups().isEmpty());

        Group aliceGroup = alicePlayer.collar.groups().nearbyGroups().get(0);
        Group bobGroup = bobPlayer.collar.groups().nearbyGroups().get(0);

        Assert.assertEquals(aliceGroup.id, bobGroup.id);

        bobPlayer.collar.messaging().sendNearbyMessage(new TextMessage("Marco?"));
        alicePlayer.collar.messaging().sendNearbyMessage(new TextMessage("Polo!"));

        waitForCondition("alice receives Marco", () -> aliceMessages.lastReceivedMessage instanceof TextMessage && "Marco?".equals(((TextMessage) aliceMessages.lastReceivedMessage).content));
        waitForCondition("bob receives Polo", () -> bobMessages.lastReceivedMessage instanceof TextMessage && "Polo!".equals(((TextMessage) bobMessages.lastReceivedMessage).content));
        Assert.assertNull("eve received no messages", eveMessages.lastReceivedMessage);
    }

    @Override
    protected Set<Entity> aliceEntities() {
        return Set.of(new Entity(bobEntityId, bobPlayerId, EntityType.PLAYER), new Entity(aliceEntityId, alicePlayerId, EntityType.PLAYER));
    }

    @Override
    protected Set<Entity> bobEntities() {
        return Set.of(new Entity(aliceEntityId, alicePlayerId, EntityType.PLAYER), new Entity(bobEntityId, bobPlayerId, EntityType.PLAYER));
    }

    @Override
    protected Set<Entity> eveEntities() {
        // Eve is evil and is trying to trick us into thinking she's in proximity to Alice
        return Set.of(new Entity(aliceEntityId, alicePlayerId, EntityType.PLAYER), new Entity(eveEntityId, evePlayerId, EntityType.PLAYER));
    }
}
