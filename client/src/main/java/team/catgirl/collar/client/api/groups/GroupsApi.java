package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.GroupType;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.location.UpdateLocationRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointFailedResponse;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointSuccessResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse.RemoveWaypointFailedResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse.RemoveWaypointSuccessResponse;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class GroupsApi extends AbstractApi<GroupsListener> {
    private final ConcurrentMap<UUID, Group> groups = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, GroupInvitation> invitations = new ConcurrentHashMap<>();

    public GroupsApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * @return groups the client is a member of
     */
    public List<Group> all() {
        synchronized (this) {
            return groups.values().stream().filter(group -> group.type == GroupType.PLAYER).collect(Collectors.toList());
        }
    }

    /**
     * @return groups of players the current player is near
     */
    public List<Group> locationGroups() {
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
    public void create(List<UUID> players) {
        sender.accept(new CreateGroupRequest(collar.identity(), UUID.randomUUID(), players));
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
        sender.accept(identityStore().createJoinGroupRequest(identity(), invitation.groupId));
        invitations.remove(invitation.groupId);
    }

    /**
     * Remove the member from the group.
     * Only {@link team.catgirl.collar.api.groups.Group.MembershipRole#OWNER} can perform this action.
     * @param group to remove player from
     * @param member the member to remove
     */
    public void removeMember(Group group, Member member) {
        sender.accept(new EjectGroupMemberRequest(identity(), group.id, member.player.id));
    }

    /**
     * Add a shared {@link team.catgirl.collar.api.waypoints.Waypoint} to the group
     * @param group to add waypoint to
     * @param name of the waypoint
     * @param location of the waypoint
     */
    public void addWaypoint(Group group, String name, Location location) {
        sender.accept(new CreateWaypointRequest(identity(), group.id, name, location));
    }

    /**
     * Remove a shared {@link Waypoint} from a group
     * @param group to the waypoint belongs to
     * @param waypoint the waypoint to remove
     */
    public void removeWaypoint(Group group, Waypoint waypoint) {
        sender.accept(new RemoveWaypointRequest(identity(), group.id, waypoint.id));
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
                groups.put(response.group.id, response.group);
                fireListener("onGroupCreated", groupsListener -> {
                    groupsListener.onGroupCreated(collar, this, response.group);
                });
                fireListener("onGroupJoined", groupsListener -> {
                    groupsListener.onGroupJoined(collar, this, response.group, collar.player());
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
                if (response.groupType == GroupType.PLAYER) {
                    invitations.put(invitation.groupId, invitation);
                    fireListener("onGroupInvited", groupsListener -> {
                        groupsListener.onGroupInvited(collar, this, invitation);
                    });
                } else if (response.groupType == GroupType.NEARBY) {
                    // Auto-accept invitations from location typed groups
                    accept(invitation);
                }
            }
            return true;
        } else if (resp instanceof CreateWaypointResponse) {
            synchronized (this) {
                if (resp instanceof CreateWaypointSuccessResponse) {
                    CreateWaypointSuccessResponse response = (CreateWaypointSuccessResponse)resp;
                    Group group = groups.get(response.groupId);
                    if (group != null) {
                        group = group.addWaypoint(response.waypoint);
                        groups.put(group.id, group);
                        Group finalGroup = group;
                        fireListener("onWaypointCreatedSuccess", groupListener -> {
                            groupListener.onWaypointCreatedSuccess(collar, this, finalGroup, response.waypoint);
                        });
                    }
                    return true;
                } else if (resp instanceof CreateWaypointFailedResponse) {
                    CreateWaypointFailedResponse response = (CreateWaypointFailedResponse)resp;
                    Group group = groups.get(response.groupId);
                    if (group != null) {
                        fireListener("onWaypointCreatedFailed", groupListener -> {
                            groupListener.onWaypointCreatedFailed(collar, this, group, response.waypointName);
                        });
                    }
                    return true;
                }
            }
        } else if (resp instanceof RemoveWaypointResponse) {
            synchronized (this) {
                if (resp instanceof RemoveWaypointSuccessResponse) {
                    RemoveWaypointSuccessResponse response = (RemoveWaypointSuccessResponse) resp;
                    Group group = groups.get(response.groupId);
                    if (group != null) {
                        Waypoint waypoint = group.waypoints.get(response.waypointId);
                        if (waypoint != null) {
                            group = group.removeWaypoint(response.waypointId);
                            if (group != null) {
                                Group finalGroup = group;
                                groups.put(group.id, group);
                                fireListener("onWaypointRemovedSuccess", groupListener -> {
                                    groupListener.onWaypointRemovedSuccess(collar, this, finalGroup, waypoint);
                                });
                            }
                        }
                    }
                    return true;
                }
                if (resp instanceof RemoveWaypointFailedResponse) {
                    RemoveWaypointFailedResponse response = (RemoveWaypointFailedResponse) resp;
                    Group group = groups.get(response.groupId);
                    if (group != null) {
                        Waypoint waypoint = group.waypoints.get(response.waypointId);
                        if (waypoint != null) {
                            fireListener("RemoveWaypointFailedResponse", groupListener -> {
                                groupListener.onWaypointRemovedFailed(collar, this, group, waypoint);
                            });
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void updatePosition(UpdateLocationRequest req) {
        sender.accept(req);
    }
}
