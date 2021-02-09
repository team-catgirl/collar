package team.catgirl.collar.tests.groups;

import org.junit.Test;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.groups.GroupInvitation;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.api.groups.GroupsListener;
import team.catgirl.collar.tests.junit.CollarTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class GroupsTest extends CollarTest {
    @Test
    public void createGroup() throws InterruptedException {

        AtomicReference<Boolean> groupCreated = new AtomicReference<>();
        GroupsListener aliceListener = new GroupsListener() {
            @Override
            public void onGroupCreated(Collar collar, GroupsApi groupsApi, Group group) {
                groupCreated.set(true);
            }
        };

        AtomicReference<GroupInvitation> bobInvitation = new AtomicReference<>();
        AtomicReference<Boolean> bobJoinedGroup = new AtomicReference<>();
        GroupsListener bobListener = new GroupsListener() {
            @Override
            public void onGroupInvited(Collar collar, GroupsApi groupsApi, GroupInvitation invitation) {
                bobInvitation.set(invitation);
                groupsApi.accept(invitation);
            }

            @Override
            public void onGroupJoined(Collar collar, GroupsApi groupsApi, Group group) {
                bobJoinedGroup.set(true);
            }
        };

        AtomicReference<GroupInvitation> eveInvitation = new AtomicReference<>();
        AtomicReference<Boolean> eveJoinedGroup = new AtomicReference<>();
        GroupsListener eveListener = new GroupsListener() {
            @Override
            public void onGroupInvited(Collar collar, GroupsApi groupsApi, GroupInvitation invitation) {
                eveInvitation.set(invitation);
                groupsApi.accept(invitation);
            }

            @Override
            public void onGroupJoined(Collar collar, GroupsApi groupsApi, Group group) {
                eveJoinedGroup.set(true);
            }
        };

        alicePlayer.collar.groups().subscribe(aliceListener);
        bobPlayer.collar.groups().subscribe(bobListener);
        evePlayer.collar.groups().subscribe(eveListener);

        // Alice creates a new group with bob and eve
        alicePlayer.collar.groups().create(List.of(bobPlayerId, evePlayerId));

        // Check that Eve and Bob recieved their invitations
        waitForCondition("Eve invite received", () -> eveInvitation.get() != null);
        waitForCondition("Bob invite received", () -> bobInvitation.get() != null);

        // Accept the invitation
        bobPlayer.collar.groups().accept(bobInvitation.get());
        evePlayer.collar.groups().accept(eveInvitation.get());

        waitForCondition("Eve joined group", eveJoinedGroup::get);
        waitForCondition("Bob joined group", bobJoinedGroup::get);

        Group theGroup = alicePlayer.collar.groups().all().get(0);
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

        System.out.println(alicePlayer.collar.identity());
        alicePlayer.collar.groups().removeMember(theGroup, eveMember);

        waitForCondition("eve is no longer a member", () -> evePlayer.collar.groups().all().size() == 0);
        waitForCondition("eve is no longer in alice's group state", () -> !alicePlayer.collar.groups().all().get(0).containsMember(evePlayerId));
        waitForCondition("eve is no longer in bob's group state", () -> !bobPlayer.collar.groups().all().get(0).containsMember(evePlayerId));
    }
}
