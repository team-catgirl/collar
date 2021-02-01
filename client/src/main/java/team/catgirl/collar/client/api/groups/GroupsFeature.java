package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.features.AbstractFeature;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GroupsFeature extends AbstractFeature<GroupListener> {
    private final ConcurrentMap<UUID, Group> groups = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, GroupInvitation> invitations = new ConcurrentHashMap<>();

    public GroupsFeature(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * Create a group with other players
     * @param players players
     */
    public void create(List<UUID> players) {
        sender.accept(new CreateGroupRequest(identity(), players));
    }

    /**
     * Leave the group
     * @param group the group to leave
     */
    public void leave(Group group) {
        sender.accept(new LeaveGroupRequest(identity(), group.id));
    }

    /**
     * Invite players to a group
     * @param group to invite players to
     * @param players to invite
     */
    public void invite(Group group, List<UUID> players) {
        sender.accept(new GroupInviteRequest(identity(), group.id, players));
    }

    /**
     * Invite players to a group
     * @param group to invite players to
     * @param players to invite
     */
    public void invite(Group group, UUID... players) {
        invite(group, Arrays.asList(players));
    }

    /**
     * Accept an invitation
     * @param invitation to accept
     */
    public void accept(GroupInvitation invitation) {
        sender.accept(new AcceptGroupMembershipRequest(identity(), invitation.groupId, Group.MembershipState.ACCEPTED));
    }

    /**
     * @return groups the client is a member of
     */
    public List<Group> groups() {
        return new ArrayList<>(groups.values());
    }

    /**
     * @return pending invitations
     */
    public List<GroupInvitation> invitations() {
        return new ArrayList<>(invitations.values());
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof CreateGroupResponse) {
            CreateGroupResponse response = (CreateGroupResponse)resp;
            groups.put(response.group.id, response.group);
            fireListener(groupListener -> {
                groupListener.onGroupCreated(collar, this, response.group);
            });
            return true;
        } else if (resp instanceof AcceptGroupMembershipResponse) {
            AcceptGroupMembershipResponse response = (AcceptGroupMembershipResponse)resp;
            groups.put(response.group.id, response.group);
            fireListener(groupListener -> {
                groupListener.onGroupJoined(collar, this, response.group);
            });
            return true;
        } else if (resp instanceof GroupInviteResponse) {
            GroupInviteResponse response = (GroupInviteResponse)resp;
            Group group = groups.get(response.groupId);
            fireListener(groupListener -> {
                groupListener.onGroupMemberInvitationsSent(collar, this, group);
            });
            return true;
        } else if (resp instanceof LeaveGroupResponse) {
            LeaveGroupResponse response = (LeaveGroupResponse)resp;
            Group group = groups.remove(response.groupId);
            fireListener(groupListener -> {
                groupListener.onGroupLeft(collar, this, group);
            });
            return true;
        } else if (resp instanceof UpdateGroupMemberPositionResponse) {
            UpdateGroupMemberPositionResponse response = (UpdateGroupMemberPositionResponse)resp;
            response.groups.forEach(group -> {
                fireListener(groupListener -> {
                    groupListener.onGroupMemberPositionUpdated(collar, this, group);
                });
            });
            return true;
        } else if (resp instanceof GroupMembershipRequest) {
            GroupMembershipRequest request = (GroupMembershipRequest)resp;
            GroupInvitation invitation = GroupInvitation.from(request);
            invitations.put(invitation.groupId, invitation);
            fireListener(groupListener -> {
                groupListener.onGroupInvited(collar, this, invitation);
            });
            return true;
        }
        return false;
    }
}
