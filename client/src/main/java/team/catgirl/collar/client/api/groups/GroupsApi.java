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
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointFailedResponse;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointSuccessResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse.RemoveWaypointFailedResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse.RemoveWaypointSuccessResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
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
        return new ArrayList<>(groups.values());
    }

    /**
     * @return pending invitations
     */
    public List<GroupInvitation> invitations() {
        return new ArrayList<>(invitations.values());
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
     * Remove the member from the group.
     * Only {@link team.catgirl.collar.api.groups.Group.MembershipRole#OWNER} can perform this action.
     * @param group to remove player from
     * @param member the member to remove
     */
    public void removeMember(Group group, Member member) {
        sender.accept(new RemoveGroupMemberRequest(identity(), group.id, member.player.id));
    }

    /**
     * Start sharing your coordinates with a group
     * @param group to share with
     */
    public void startSharingCoordinates(Group group) {
        sharingState.put(group.id, CoordinateSharingState.SHARING);
        startOrStopSharingPosition();
    }

    /**
     * Start sharing your coordinates with a group
     * @param group to stop sharing with
     */
    public void stopSharingCoordinates(Group group) {
        sharingState.put(group.id, CoordinateSharingState.NOT_SHARING);
        startOrStopSharingPosition();
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
        CoordinateSharingState coordinateSharingState = sharingState.get(group.id);
        return coordinateSharingState == CoordinateSharingState.SHARING;
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof CreateGroupResponse) {
            CreateGroupResponse response = (CreateGroupResponse)resp;
            updateState(response.group.id,  group -> response.group).ifPresent(group -> {
                fireListener("onGroupCreated", groupsListener -> {
                    groupsListener.onGroupCreated(collar, this, response.group);
                });
                startOrStopSharingPosition();
            });
            return true;
        } else if (resp instanceof AcceptGroupMembershipResponse) {
            AcceptGroupMembershipResponse response = (AcceptGroupMembershipResponse)resp;
            updateState(response.group.id, group -> response.group).ifPresent(group -> {
                fireListener("onGroupJoined", groupsListener -> {
                    groupsListener.onGroupJoined(collar, this, response.group);
                });
                startOrStopSharingPosition();
            });
            return true;
        } else if (resp instanceof GroupInviteResponse) {
            GroupInviteResponse response = (GroupInviteResponse)resp;
            updateState(response.groupId, group -> group).ifPresent(group -> {
                fireListener("onGroupMemberInvitationsSent", groupsListener -> {
                    groupsListener.onGroupMemberInvitationsSent(collar, this, group);
                });
                startOrStopSharingPosition();
            });
            return true;
        } else if (resp instanceof LeaveGroupResponse) {
            LeaveGroupResponse response = (LeaveGroupResponse)resp;
            updateState(response.groupId, group -> null).ifPresent(group -> {
                fireListener("onGroupLeft", groupsListener -> {
                    groupsListener.onGroupLeft(collar, this, group);
                });
                startOrStopSharingPosition();
            });
            return true;
        } else if (resp instanceof GroupChangedResponse) {
            GroupChangedResponse response = (GroupChangedResponse)resp;
            response.groups.forEach(group -> {
                updateState(group.id, oldGroup -> group).ifPresent(updatedGroup -> {
                    fireListener("onGroupUpdated", groupsListener -> {
                        groupsListener.onGroupUpdated(collar, this, updatedGroup);
                    });
                    startOrStopSharingPosition();
                });
            });
            return true;
        } else if (resp instanceof GroupMembershipRequest) {
            GroupMembershipRequest request = (GroupMembershipRequest)resp;
            GroupInvitation invitation = GroupInvitation.from(request);
            invitations.put(invitation.groupId, invitation);
            fireListener("onGroupInvited", groupsListener -> {
                groupsListener.onGroupInvited(collar, this, invitation);
            });
            startOrStopSharingPosition();
            return true;
        } else if (resp instanceof RemoveGroupMemberResponse) {
            RemoveGroupMemberResponse response = (RemoveGroupMemberResponse)resp;
            Group group = this.groups.get(response.groupId);
            MinecraftPlayer minecraftPlayer = group.members.keySet().stream()
                    .filter(player -> response.player.equals(player.id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find player " + response.player));
            updateState(response.groupId, groupState -> groupState.removeMember(minecraftPlayer)).ifPresent(newGroup -> {
                fireListener("onGroupMemberRemoved", groupListener -> {
                    groupListener.onGroupMemberRemoved(collar, newGroup, minecraftPlayer);
                });
            });
            return true;
        } else if (resp instanceof CreateWaypointResponse) {
            if (resp instanceof CreateWaypointSuccessResponse) {
                CreateWaypointSuccessResponse response = (CreateWaypointSuccessResponse)resp;
                updateState(response.groupId, group -> group.addWaypoint(response.waypoint)).ifPresent(group1 -> {
                    fireListener("onWaypointCreatedSuccess", groupListener -> {
                        groupListener.onWaypointCreatedSuccess(collar, this, group1, response.waypoint);
                    });
                });
                return true;
            } else if (resp instanceof CreateWaypointFailedResponse) {
                CreateWaypointFailedResponse response = (CreateWaypointFailedResponse)resp;
                updateState(response.groupId, group -> group.removeWaypoint(response.groupId)).ifPresent(group1 -> {
                    fireListener("onWaypointCreatedFailed", groupListener -> {
                        groupListener.onWaypointCreatedFailed(collar, this, group1, response.waypointName);
                    });
                });
                return true;
            }
        } else if (resp instanceof RemoveWaypointResponse) {
            if (resp instanceof RemoveWaypointSuccessResponse) {
                RemoveWaypointSuccessResponse response = (RemoveWaypointSuccessResponse) resp;
                Waypoint waypoint = groups.get(response.groupId).waypoints.get(response.waypointId);
                updateState(response.groupId, group -> group.removeWaypoint(response.waypointId)).ifPresent(group -> {
                    fireListener("onWaypointRemovedSuccess", groupListener -> {
                        groupListener.onWaypointRemovedSuccess(collar, this, group, waypoint);
                    });
                });
                return true;
            }
            if (resp instanceof RemoveWaypointFailedResponse) {
                RemoveWaypointFailedResponse response = (RemoveWaypointFailedResponse) resp;
                updateState(response.groupId, group -> group.removeWaypoint(response.waypointId)).ifPresent(group -> {
                    fireListener("onWaypointRemovedFailed", groupListener -> {
                        groupListener.onWaypointRemovedFailed(collar, this, group, null);
                    });
                });
                return true;
            }
        }
        return false;
    }

    private void updatePosition(UpdateGroupMemberPositionRequest req) {
        sender.accept(req);
    }

    public Optional<Group> updateState(UUID groupId, Function<Group, Group> updater) {
        synchronized (groupId) {
            Group oldValue = groups.get(groupId);
            Group newValue = updater.apply(oldValue);
            if (newValue != null) {
                groups.put(groupId, newValue);
            }
            return newValue == null ? Optional.of(oldValue) : Optional.of(newValue);
        }
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
                    .findFirst().ifPresent(group -> groupsApi.updatePosition(new UpdateGroupMemberPositionRequest(identity, position.get()))
                );
            }, 0, 10, TimeUnit.SECONDS);
        }

        public void stop() {
            groupsApi.updatePosition(new UpdateGroupMemberPositionRequest(identity, Location.UNKNOWN));
            if (this.scheduler != null) {
                this.scheduler.shutdown();
            }
        }
    }
}
