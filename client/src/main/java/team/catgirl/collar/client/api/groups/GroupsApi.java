package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.features.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.location.ClearLocationRequest;
import team.catgirl.collar.protocol.location.UpdateLocationRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointFailedResponse;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointSuccessResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse.RemoveWaypointFailedResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse.RemoveWaypointSuccessResponse;
import team.catgirl.collar.security.ClientIdentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GroupsApi extends AbstractApi<GroupsListener> {
    private final ConcurrentMap<UUID, Group> groups = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, GroupInvitation> invitations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CoordinateSharingState> sharingState = new ConcurrentHashMap<>();
    private final Supplier<Location> positionSupplier;
    private PositionUpdater positionUpdater;

    public GroupsApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender, Supplier<Location> positionSupplier) {
        super(collar, identityStoreSupplier, sender);
        this.positionSupplier = positionSupplier;
    }

    /**
     * @return groups the client is a member of
     */
    public List<Group> all() {
        synchronized (this) {
            return new ArrayList<>(groups.values());
        }
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
        sender.accept(new JoinGroupRequest(identity(), invitation.groupId, Group.MembershipState.ACCEPTED));
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
     * Start sharing your coordinates with a group
     * @param group to share with
     */
    public void startSharingCoordinates(Group group) {
        synchronized (this) {
            sharingState.put(group.id, CoordinateSharingState.SHARING);
            startOrStopSharingPosition();
        }
    }

    /**
     * Start sharing your coordinates with a group
     * @param group to stop sharing with
     */
    public void stopSharingCoordinates(Group group) {
        synchronized (this) {
            sharingState.put(group.id, CoordinateSharingState.NOT_SHARING);
            startOrStopSharingPosition();
            sender.accept(new ClearLocationRequest(identity()));
        }
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

    /**
     * Tests if you are currently sharing with the group
     * @param group to test
     * @return sharing
     */
    public boolean isSharingCoordinatesWith(Group group) {
        synchronized (this) {
            CoordinateSharingState coordinateSharingState = sharingState.get(group.id);
            return coordinateSharingState == CoordinateSharingState.SHARING;
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
                startOrStopSharingPosition();
            }
            return true;
        } else if (resp instanceof JoinGroupResponse) {
            synchronized (this) {
                JoinGroupResponse response = (JoinGroupResponse)resp;
                groups.put(response.group.id, response.group);
                fireListener("onGroupJoined", groupsListener -> {
                    groupsListener.onGroupJoined(collar, this, response.group, response.player);
                });
                invitations.remove(response.group.id);
                startOrStopSharingPosition();
            }
            return true;
        } else if (resp instanceof LeaveGroupResponse) {
            synchronized (this) {
                LeaveGroupResponse response = (LeaveGroupResponse)resp;
                if (response.player.equals(collar.player())) {
                    // Remove myself from the group
                    Group removed = groups.remove(response.groupId);
                    if (removed != null) {
                        fireListener("onGroupLeft", groupsListener -> {
                            groupsListener.onGroupLeft(collar, this, removed, response.player);
                        });
                        startOrStopSharingPosition();
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
                        startOrStopSharingPosition();
                    }
                }
            }
            return true;
        } else if (resp instanceof GroupInviteResponse) {
            synchronized (this) {
                GroupInviteResponse request = (GroupInviteResponse)resp;
                GroupInvitation invitation = GroupInvitation.from(request);
                invitations.put(invitation.groupId, invitation);
                fireListener("onGroupInvited", groupsListener -> {
                    groupsListener.onGroupInvited(collar, this, invitation);
                });
                startOrStopSharingPosition();
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

    private void startOrStopSharingPosition() {
        // Clean these out
        if (groups.isEmpty()) {
            sharingState.clear();
        }
        // Update the position
        if (positionUpdater != null) {
            if (groups.isEmpty() && positionUpdater.isRunning()) {
                positionUpdater.stop();
                positionUpdater = null;
            } else if (!positionUpdater.isRunning()) {
                positionUpdater.start();
            }
        } else if (!groups.isEmpty()) {
            positionUpdater = new PositionUpdater(identity(), this, positionSupplier);
            positionUpdater.start();
        }
    }

    public enum CoordinateSharingState {
        SHARING,
        NOT_SHARING
    }

    static class PositionUpdater {
        private final ClientIdentity identity;
        private final GroupsApi groupsApi;
        private final Supplier<Location> position;
        private ScheduledExecutorService scheduler;

        public PositionUpdater(ClientIdentity identity, GroupsApi groupsApi, Supplier<Location> position) {
            this.identity = identity;
            this.groupsApi = groupsApi;
            this.position = position;
        }

        public boolean isRunning() {
            return !scheduler.isShutdown();
        }

        public void start() {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                groupsApi.all().stream()
                    .filter(groupsApi::isSharingCoordinatesWith)
                    .findFirst()
                    .ifPresent(this::updateLocation);
            }, 0, 10, TimeUnit.SECONDS);
        }

        public void stop() {
            groupsApi.updatePosition(new UpdateLocationRequest(identity, groupsApi.collar.player(), Location.UNKNOWN));
            if (this.scheduler != null) {
                this.scheduler.shutdown();
            }
        }

        private void updateLocation(Group group) {
            groupsApi.updatePosition(new UpdateLocationRequest(identity, groupsApi.collar.player(), position.get()));
        }
    }
}
