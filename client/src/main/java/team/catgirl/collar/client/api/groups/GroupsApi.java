package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.api.location.Position;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.features.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

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
    private final Supplier<Position> positionSupplier;
    private PositionUpdater positionUpdater;

    public GroupsApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender, Supplier<Position> positionSupplier) {
        super(collar, identityStoreSupplier, sender);
        this.positionSupplier = positionSupplier;
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
     * Remove the member from the group
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
            groups.put(response.group.id, response.group);
            fireListener("onGroupCreated", groupsListener -> {
                groupsListener.onGroupCreated(collar, this, response.group);
            });
            startOrStopSharingPosition();
            return true;
        } else if (resp instanceof AcceptGroupMembershipResponse) {
            AcceptGroupMembershipResponse response = (AcceptGroupMembershipResponse)resp;
            groups.put(response.group.id, response.group);
            fireListener("onGroupJoined", groupsListener -> {
                groupsListener.onGroupJoined(collar, this, response.group);
            });
            startOrStopSharingPosition();
            return true;
        } else if (resp instanceof GroupInviteResponse) {
            GroupInviteResponse response = (GroupInviteResponse)resp;
            Group group = groups.get(response.groupId);
            if (group == null) {
                return false;
            }
            fireListener("onGroupMemberInvitationsSent", groupsListener -> {
                groupsListener.onGroupMemberInvitationsSent(collar, this, group);
            });
            startOrStopSharingPosition();
            return true;
        } else if (resp instanceof LeaveGroupResponse) {
            LeaveGroupResponse response = (LeaveGroupResponse)resp;
            Group group = groups.remove(response.groupId);
            if (group == null) {
                return false;
            }
            fireListener("onGroupLeft", groupsListener -> {
                groupsListener.onGroupLeft(collar, this, group);
            });
            startOrStopSharingPosition();
            return true;
        } else if (resp instanceof UpdateGroupsUpdatedResponse) {
            UpdateGroupsUpdatedResponse response = (UpdateGroupsUpdatedResponse)resp;
            response.groups.forEach(group -> {
                fireListener("onGroupsUpdated", groupsListener -> {
                    groupsListener.onGroupUpdated(collar, this, group);
                });
            });
            startOrStopSharingPosition();
            return true;
        } else if (resp instanceof GroupMembershipRequest) {
            GroupMembershipRequest request = (GroupMembershipRequest)resp;
            GroupInvitation invitation = GroupInvitation.from(request);
            invitations.put(invitation.groupId, invitation);
            fireListener("GroupMembershipRequest", groupsListener -> {
                groupsListener.onGroupInvited(collar, this, invitation);
            });
            startOrStopSharingPosition();
            return true;
        } else if (resp instanceof RemoveGroupMemberResponse) {
            RemoveGroupMemberResponse response = (RemoveGroupMemberResponse)resp;
            Group group = groups.get(response.groupId);
            if (group == null) {
                return false;
            }
            MinecraftPlayer minecraftPlayer = group.members.keySet().stream()
                    .filter(player -> response.player.equals(player.id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find player " + response.player));
            fireListener("GroupMembershipRequest", groupListener -> {
                groupListener.onGroupMemberRemoved(collar, group, minecraftPlayer);
            });
        }
        return false;
    }

    private void updatePosition(UpdateGroupMemberPositionRequest req) {
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
        private final Supplier<Position> position;
        private ScheduledExecutorService scheduler;

        public PositionUpdater(ClientIdentity identity, GroupsApi groupsApi, Supplier<Position> position) {
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
                groupsApi.groups().stream()
                    .filter(groupsApi::isSharingCoordinatesWith)
                    .findFirst().ifPresent(group -> groupsApi.updatePosition(new UpdateGroupMemberPositionRequest(identity, position.get()))
                );
            }, 0, 10, TimeUnit.SECONDS);
        }

        public void stop() {
            groupsApi.updatePosition(new UpdateGroupMemberPositionRequest(identity, Position.UNKNOWN));
            if (this.scheduler != null) {
                this.scheduler.shutdown();
            }
        }
    }
}
