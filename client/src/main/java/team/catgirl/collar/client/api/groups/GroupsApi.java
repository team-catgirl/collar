package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.friends.Status;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.GroupType;
import team.catgirl.collar.api.groups.Member;
import team.catgirl.collar.api.groups.MembershipRole;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.sdht.SDHTApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class GroupsApi extends AbstractApi<GroupsListener> {
    private final ConcurrentMap<UUID, Group> groups = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, GroupInvitation> invitations = new ConcurrentHashMap<>();
    private final SDHTApi sdhtApi;

    public GroupsApi(Collar collar,
                     Supplier<ClientIdentityStore> identityStoreSupplier,
                     Consumer<ProtocolRequest> sender,
                     SDHTApi sdhtApi) {
        super(collar, identityStoreSupplier, sender);
        this.sdhtApi = sdhtApi;
    }

    /**
     * @return groups the client is a member of
     */
    public List<Group> all() {
        synchronized (this) {
            return groups.values().stream().filter(group -> group.type == GroupType.PARTY).collect(Collectors.toList());
        }
    }

    /**
     * @return groups of players the current player is near
     */
    public List<Group> nearbyGroups() {
        synchronized (this) {
            return groups.values().stream().filter(group -> group.type == GroupType.NEARBY).collect(Collectors.toList());
        }
    }

    /**
     * Find a group by its ID
     * @param groupId of the group to find
     * @return group
     */
    public Optional<Group> findGroupById(UUID groupId) {
        return groups.values().stream().filter(group -> group.id.equals(groupId)).findFirst();
    }

    /**
     * @return pending invitations
     */
    public List<GroupInvitation> invitations() {
        synchronized (this) {
            return new ArrayList<>(invitations.values());
        }
    }

    /**
     * Create a group with other players
     * @param players players
     */
    public void create(String name, GroupType type, List<UUID> players) {
        sender.accept(new CreateGroupRequest(collar.identity(), UUID.randomUUID(), name, type, players));
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
        sender.accept(identityStore().createJoinGroupRequest(identity(), invitation.group));
        invitations.remove(invitation.group);
    }

    /**
     * Remove the member from the group.
     * Only {@link MembershipRole#OWNER} can perform this action.
     * @param group to remove player from
     * @param member the member to remove
     */
    public void removeMember(Group group, Member member) {
        sender.accept(new EjectGroupMemberRequest(identity(), group.id, member.player.minecraftPlayer.id));
    }

    /**
     * Delete a group
     * @param group to delete
     */
    public void delete(Group group) {
        sender.accept(new DeleteGroupRequest(identity(), group.id));
    }

    /**
     * Transfer ownership of the group to another player
     * @param group to transfer
     * @param player to transfer to
     */
    public void transferOwnership(Group group, Player player) {
        sender.accept(new TransferGroupOwnershipRequest(identity(), group.id, player.profile));
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.DISCONNECTED) {
            synchronized (this) {
                groups.clear();
                invitations.clear();
            }
        }
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof CreateGroupResponse) {
            synchronized (this) {
                CreateGroupResponse response = (CreateGroupResponse)resp;
                Group group = response.group;
                groups.put(response.group.id, group);
                fireListener("onGroupCreated", groupsListener -> {
                    groupsListener.onGroupCreated(collar, this, group);
                });
                fireListener("onGroupJoined", groupsListener -> {
                    groupsListener.onGroupJoined(collar, this, group, collar.player());
                });
            }
            return true;
        } else if (resp instanceof JoinGroupResponse) {
            synchronized (this) {
                JoinGroupResponse response = (JoinGroupResponse) resp;
                AcknowledgedGroupJoinedRequest request = identityStore().processJoinGroupResponse(response);
                sender.accept(request);
            }
            return true;
        } else if (resp instanceof AcknowledgedGroupJoinedResponse) {
            AcknowledgedGroupJoinedResponse response = (AcknowledgedGroupJoinedResponse) resp;
            groups.put(response.group.id, response.group);
            invitations.remove(response.group.id);
            if (groups.containsKey(response.group.id)) {
                identityStore().processAcknowledgedGroupJoinedResponse(response);
            }
            if (response.player.equals(collar.player())) {
                sdhtApi.table.sync(response.group.id);
            }
            fireListener("onGroupJoined", groupsListener -> {
                groupsListener.onGroupJoined(collar, this, response.group, response.player);
            });
        } else if (resp instanceof LeaveGroupResponse) {
            synchronized (this) {
                LeaveGroupResponse response = (LeaveGroupResponse)resp;
                if (response.sender == null || response.sender.equals(collar.identity())) {
                    // Remove myself from the group
                    Group removed = groups.remove(response.groupId);
                    if (removed != null) {
                        sdhtApi.table.remove(removed.id);
                        fireListener("onGroupLeft", groupsListener -> {
                            groupsListener.onGroupLeft(collar, this, removed, response.player);
                        });
                    }
                    invitations.remove(response.groupId);
                } else {
                    // Remove another player from the group
                    Group group = groups.get(response.groupId);
                    if (group != null) {
                        Group updatedGroup = group.removeMember(response.player);
                        groups.put(updatedGroup.id, updatedGroup);
                        fireListener("onGroupLeft", groupsListener -> {
                            groupsListener.onGroupLeft(collar, this, updatedGroup, response.player);
                        });
                    }
                }
                // Remove the group sessions of the player who left the group so that any messages they send
                // will no longer be decrypted by the client.
                // When this method is called after the client itself has left, it removes the session for the group
                // until they are invited to join the same group again
                identityStore().processLeaveGroupResponse(response);
            }
            return true;
        } else if (resp instanceof GroupInviteResponse) {
            synchronized (this) {
                GroupInviteResponse response = (GroupInviteResponse) resp;
                GroupInvitation invitation = GroupInvitation.from(response);
                switch (response.type) {
                    case GROUP:
                    case PARTY:
                        invitations.put(invitation.group, invitation);
                        fireListener("onGroupInvited", groupsListener -> {
                            groupsListener.onGroupInvited(collar, this, invitation);
                        });
                        break;
                    case NEARBY:
                        // Auto-accept invitations from location typed groups
                        accept(invitation);
                        break;
                    default:
                        throw new IllegalStateException("unknown group type" + response.type);
                }
            }
            return true;
        } else if (resp instanceof RejoinGroupResponse) {
            // Rejoin the group
            sender.accept(identityStore().createJoinGroupRequest(identity(), ((RejoinGroupResponse) resp).group));
        } else if (resp instanceof UpdateGroupMemberResponse) {
            synchronized (this) {
                UpdateGroupMemberResponse response = (UpdateGroupMemberResponse) resp;
                Group group = groups.get(response.groupId);
                if (group != null) {
                    Group updatedGroup;
                    if (response.status == Status.OFFLINE) {
                        updatedGroup = group.updatePlayer(response.player);
                    } else if (response.role != null) {
                        updatedGroup = group.updateMembershipRole(response.player, response.role);
                    } else {
                        updatedGroup = null;
                    }
                    if (updatedGroup != null) {
                        groups.put(updatedGroup.id, updatedGroup);
                        fireListener("onGroupMemberUpdated", groupsListener -> {
                            groupsListener.onGroupMemberUpdated(collar, this, updatedGroup, response.player);
                        });
                    }
                }
            }
            return true;
        }
        return false;
    }
}
