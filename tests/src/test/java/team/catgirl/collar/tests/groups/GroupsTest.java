package team.catgirl.collar.tests.groups;

import org.junit.Before;
import org.junit.Test;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Member;
import team.catgirl.collar.api.location.Dimension;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.api.messaging.Message;
import team.catgirl.collar.api.messaging.TextMessage;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.groups.GroupInvitation;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.api.groups.GroupsListener;
import team.catgirl.collar.client.api.messaging.MessagingApi;
import team.catgirl.collar.client.api.messaging.MessagingListener;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.tests.junit.CollarTest;

import java.util.List;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class GroupsTest extends CollarTest {

    TestGroupsListener aliceListener;
    TestGroupsListener bobListener;
    TestGroupsListener eveListener;

    @Before
    public void createListeners() {
        aliceListener = new TestGroupsListener();
        alicePlayer.collar.groups().subscribe(aliceListener);

        bobListener = new TestGroupsListener();
        bobPlayer.collar.groups().subscribe(bobListener);

        eveListener = new TestGroupsListener();
        evePlayer.collar.groups().subscribe(eveListener);
    }

    @Test
    public void createGroupAndPerformMembershipActions() {

        // Alice creates a new group with bob and eve
        alicePlayer.collar.groups().create(List.of(bobPlayerId, evePlayerId));

        // Check that Eve and Bob received their invitations
        waitForCondition("Eve invite received", () -> eveListener.invitation != null);
        waitForCondition("Bob invite received", () -> bobListener.invitation != null);

        // Accept the invitation
        bobPlayer.collar.groups().accept(bobListener.invitation);
        evePlayer.collar.groups().accept(eveListener.invitation);

        waitForCondition("Eve joined group", () -> eveListener.joinedGroup);
        waitForCondition("Bob joined group", () -> bobListener.joinedGroup);

        Group theGroup = alicePlayer.collar.groups().all().get(0);

        // Find eve
        Member eveMember = theGroup.members.values().stream().filter(candidate -> candidate.player.id.equals(evePlayerId)).findFirst().orElseThrow();

        waitForCondition("eve is in alice's group", () -> alicePlayer.collar.groups().all().get(0).containsPlayer(evePlayerId));
        waitForCondition("eve is in bobs's group", () -> bobPlayer.collar.groups().all().get(0).containsPlayer(evePlayerId));
        waitForCondition("eve is in eve's group", () -> evePlayer.collar.groups().all().get(0).containsPlayer(evePlayerId));

        waitForCondition("alice is in alice's group", () -> alicePlayer.collar.groups().all().get(0).containsPlayer(alicePlayerId));
        waitForCondition("alice is in bobs's group", () -> bobPlayer.collar.groups().all().get(0).containsPlayer(alicePlayerId));
        waitForCondition("alice is in eve's group", () -> evePlayer.collar.groups().all().get(0).containsPlayer(alicePlayerId));

        waitForCondition("bob is in alice's group", () -> alicePlayer.collar.groups().all().get(0).containsPlayer(bobPlayerId));
        waitForCondition("bob is in bobs's group", () -> bobPlayer.collar.groups().all().get(0).containsPlayer(bobPlayerId));
        waitForCondition("bob is in eve's group", () -> evePlayer.collar.groups().all().get(0).containsPlayer(bobPlayerId));

        // Remove eve
        alicePlayer.collar.groups().removeMember(theGroup, eveMember);

        waitForCondition("eve is no longer a member", () -> evePlayer.collar.groups().all().isEmpty());
        waitForCondition("eve is no longer in alice's group state", () -> !alicePlayer.collar.groups().all().get(0).containsPlayer(evePlayerId));
        waitForCondition("eve is no longer in bob's group state", () -> !bobPlayer.collar.groups().all().get(0).containsPlayer(evePlayerId));

        // Alice leaves the group
        alicePlayer.collar.groups().leave(theGroup);

        waitForCondition("alice is no longer a member", () -> alicePlayer.collar.groups().all().isEmpty());
        waitForCondition("alice is no longer in bob's group state", () -> !bobPlayer.collar.groups().all().get(0).containsPlayer(alicePlayerId));

        bobPlayer.collar.groups().leave(theGroup);
        waitForCondition("bob is no longer a member", () -> bobPlayer.collar.groups().all().isEmpty());
        waitForCondition("eve is no longer a member", () -> evePlayer.collar.groups().all().isEmpty());
        waitForCondition("alice is no longer a member", () -> alicePlayer.collar.groups().all().isEmpty());
    }

    @Test
    public void sendGroupMessage() {
        MessagingListenerImpl aliceMessages = new MessagingListenerImpl();
        alicePlayer.collar.messaging().subscribe(aliceMessages);

        MessagingListenerImpl bobMessages = new MessagingListenerImpl();
        bobPlayer.collar.messaging().subscribe(bobMessages);

        MessagingListenerImpl eveMessages = new MessagingListenerImpl();
        evePlayer.collar.messaging().subscribe(eveMessages);

        // Alice creates a new group with bob and eve
        alicePlayer.collar.groups().create(List.of(bobPlayerId, evePlayerId));

        // Check that Eve and Bob received their invitations
        waitForCondition("Eve invite received", () -> eveListener.invitation != null);
        waitForCondition("Bob invite received", () -> bobListener.invitation != null);

        // Accept the invitation
        bobPlayer.collar.groups().accept(bobListener.invitation);
        evePlayer.collar.groups().accept(eveListener.invitation);

        waitForCondition("Eve joined group", () -> eveListener.joinedGroup);
        waitForCondition("Bob joined group", () -> bobListener.joinedGroup);

        Group theGroup = alicePlayer.collar.groups().all().get(0);

        alicePlayer.collar.messaging().sendGroupMessage(theGroup, new TextMessage("UwU"));
        waitForCondition("bob receives UwU", () -> bobMessages.lastReceivedMessage instanceof TextMessage && "UwU".equals(((TextMessage) bobMessages.lastReceivedMessage).content));
        waitForCondition("eve receives UwU", () -> eveMessages.lastReceivedMessage instanceof TextMessage && "UwU".equals(((TextMessage) eveMessages.lastReceivedMessage).content));
        waitForCondition("alice did not get UwU", () -> aliceMessages.lastReceivedMessage == null);

        bobPlayer.collar.messaging().sendGroupMessage(theGroup, new TextMessage("OwO"));
        waitForCondition("eve receives OwO", () -> eveMessages.lastReceivedMessage instanceof TextMessage && "OwO".equals(((TextMessage) eveMessages.lastReceivedMessage).content));
        waitForCondition("alice receives OwO", () -> aliceMessages.lastReceivedMessage instanceof TextMessage && "OwO".equals(((TextMessage) aliceMessages.lastReceivedMessage).content));
        waitForCondition("bobs last message was UwU", () -> bobMessages.lastReceivedMessage instanceof TextMessage && "UwU".equals(((TextMessage) bobMessages.lastReceivedMessage).content));
    }

    public static class TestGroupsListener implements GroupsListener {

        public boolean createdGroup = false;
        public boolean joinedGroup = false;
        public boolean leftGroup = false;
        public GroupInvitation invitation;

        @Override
        public void onGroupCreated(Collar collar, GroupsApi groupsApi, Group group) {
            createdGroup = true;
        }

        @Override
        public void onGroupJoined(Collar collar, GroupsApi groupsApi, Group group, MinecraftPlayer player) {
            if (collar.player().equals(player)) {
                joinedGroup = true;
            }
        }

        @Override
        public void onGroupLeft(Collar collar, GroupsApi groupsApi, Group group, MinecraftPlayer player) {
            if (collar.player().equals(player)) {
                leftGroup = true;
            }
        }

        @Override
        public void onGroupInvited(Collar collar, GroupsApi groupsApi, GroupInvitation invitation) {
            this.invitation = invitation;
        }
    }

    public static class MessagingListenerImpl implements MessagingListener {
        public Message lastSentMessage;
        public Message lastReceivedMessage;

        @Override
        public void onGroupMessageSent(Collar collar, MessagingApi messagingApi, Group group, Message message) {
            this.lastSentMessage = message;
        }

        @Override
        public void onGroupMessageReceived(Collar collar, MessagingApi messagingApi, Group group, MinecraftPlayer sender, Message message) {
            this.lastReceivedMessage = message;
        }
    }
}
