package team.catgirl.collar.tests.groups;

import org.junit.Before;
import org.junit.Test;
import spark.Response;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.api.location.Dimension;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.groups.GroupInvitation;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.api.groups.GroupsListener;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.tests.junit.CollarTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

        waitForCondition("eve is in alice's group", () -> alicePlayer.collar.groups().all().get(0).containsMember(evePlayerId));
        waitForCondition("eve is in bobs's group", () -> bobPlayer.collar.groups().all().get(0).containsMember(evePlayerId));
        waitForCondition("eve is in eve's group", () -> evePlayer.collar.groups().all().get(0).containsMember(evePlayerId));

        waitForCondition("alice is in alice's group", () -> alicePlayer.collar.groups().all().get(0).containsMember(alicePlayerId));
        waitForCondition("alice is in bobs's group", () -> bobPlayer.collar.groups().all().get(0).containsMember(alicePlayerId));
        waitForCondition("alice is in eve's group", () -> evePlayer.collar.groups().all().get(0).containsMember(alicePlayerId));

        waitForCondition("bob is in alice's group", () -> alicePlayer.collar.groups().all().get(0).containsMember(bobPlayerId));
        waitForCondition("bob is in bobs's group", () -> bobPlayer.collar.groups().all().get(0).containsMember(bobPlayerId));
        waitForCondition("bob is in eve's group", () -> evePlayer.collar.groups().all().get(0).containsMember(bobPlayerId));

        // Remove eve
        alicePlayer.collar.groups().removeMember(theGroup, eveMember);

        waitForCondition("eve is no longer a member", () -> evePlayer.collar.groups().all().isEmpty());
        waitForCondition("eve is no longer in alice's group state", () -> !alicePlayer.collar.groups().all().get(0).containsMember(evePlayerId));
        waitForCondition("eve is no longer in bob's group state", () -> !bobPlayer.collar.groups().all().get(0).containsMember(evePlayerId));

        // Alice leaves the group
        alicePlayer.collar.groups().leave(theGroup);

        waitForCondition("alice is no longer a member", () -> alicePlayer.collar.groups().all().isEmpty());
        waitForCondition("alice is no longer in bob's group state", () -> !bobPlayer.collar.groups().all().get(0).containsMember(alicePlayerId));

        bobPlayer.collar.groups().leave(theGroup);
        waitForCondition("bob is no longer a member", () -> bobPlayer.collar.groups().all().isEmpty());
        waitForCondition("eve is no longer a member", () -> evePlayer.collar.groups().all().isEmpty());
        waitForCondition("alice is no longer a member", () -> alicePlayer.collar.groups().all().isEmpty());
    }

    @Test
    public void waypoints() {
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

        // Check there are zero waypoints
        waitForCondition("alice does not have a waypoint", () -> alicePlayer.collar.groups().all().get(0).waypoints.isEmpty());
        waitForCondition("bob does not have a waypoint", () -> bobPlayer.collar.groups().all().get(0).waypoints.isEmpty());
        waitForCondition("bob does not have a waypoint", () -> evePlayer.collar.groups().all().get(0).waypoints.isEmpty());

        // Bob creates a waypoint
        Location baseLocation = new Location(0d, 64d, 0d, Dimension.OVERWORLD);
        bobPlayer.collar.groups().addWaypoint(theGroup, "Our base", baseLocation);

        waitForCondition("alice has the waypoint", () -> !alicePlayer.collar.groups().all().get(0).waypoints.isEmpty());
        waitForCondition("bob has a waypoint", () -> !bobPlayer.collar.groups().all().get(0).waypoints.isEmpty());
        waitForCondition("bob has a waypoint", () -> !evePlayer.collar.groups().all().get(0).waypoints.isEmpty());

        waitForCondition("alice has the waypoint", () -> {
            Waypoint waypoint = alicePlayer.collar.groups().all().get(0).waypoints.get(aliceListener.waypoint.id);
            return waypoint.location.equals(baseLocation) && waypoint.name.equals("Our base");
        });
        waitForCondition("bob has the waypoint", () -> {
            Waypoint waypoint = bobPlayer.collar.groups().all().get(0).waypoints.get(bobListener.waypoint.id);
            return waypoint.location.equals(baseLocation) && waypoint.name.equals("Our base");
        });

        waitForCondition("eve has the waypoint", () -> {
            Waypoint waypoint = evePlayer.collar.groups().all().get(0).waypoints.get(eveListener.waypoint.id);
            return waypoint.location.equals(baseLocation) && waypoint.name.equals("Our base");
        });

        alicePlayer.collar.groups().removeWaypoint(theGroup, aliceListener.waypoint);

        waitForCondition("alice does not have a waypoint", () -> alicePlayer.collar.groups().all().get(0).waypoints.isEmpty());
        waitForCondition("bob does not have a waypoint", () -> bobPlayer.collar.groups().all().get(0).waypoints.isEmpty());
        waitForCondition("eve does not have a waypoint", () -> evePlayer.collar.groups().all().get(0).waypoints.isEmpty());
    }

    private static class TestGroupsListener implements GroupsListener {

        boolean createdGroup = false;
        boolean joinedGroup = false;
        boolean leftGroup = false;
        GroupInvitation invitation = null;
        Waypoint waypoint;

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

        @Override
        public void onWaypointCreatedSuccess(Collar collar, GroupsApi feature, Group group, Waypoint waypoint) {
            this.waypoint = waypoint;
        }
    }
}
