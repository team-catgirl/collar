package team.catgirl.collar.tests.groups;

import org.junit.Test;
import team.catgirl.collar.api.groups.Group;
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
        GroupsListener bobListener = new GroupsListener() {
            @Override
            public void onGroupInvited(Collar collar, GroupsApi groupsApi, GroupInvitation invitation) {
                bobInvitation.set(invitation);
                groupsApi.accept(invitation);
            }
        };

        AtomicReference<GroupInvitation> eveInvitation = new AtomicReference<>();
        GroupsListener eveListener = new GroupsListener() {
            @Override
            public void onGroupInvited(Collar collar, GroupsApi groupsApi, GroupInvitation invitation) {
                eveInvitation.set(invitation);
                groupsApi.accept(invitation);
            }
        };

        alicePlayer.collar.groups().subscribe(aliceListener);
        bobPlayer.collar.groups().subscribe(bobListener);
        evePlayer.collar.groups().subscribe(eveListener);

        alicePlayer.collar.groups().create(List.of(bobPlayerId, evePlayerId));

        waitForCondition("Eve invite received", () -> eveInvitation.get() != null);
        waitForCondition("Bob invite received", () -> bobInvitation.get() != null);
    }
}
