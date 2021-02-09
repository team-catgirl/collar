package team.catgirl.collar.server.services.groups;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.location.UpdateLocationResponse;
import team.catgirl.collar.protocol.location.UpdateLocationRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointFailedResponse;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointSuccessResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse.RemoveWaypointFailedResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.session.SessionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class GroupService {

    private static final Logger LOGGER = Logger.getLogger(GroupService.class.getName());

    private final ServerIdentity serverIdentity;
    private final SessionManager sessions;

    private final ConcurrentMap<UUID, Group> groupsById = new ConcurrentHashMap<>();

    public GroupService(ServerIdentity serverIdentity, SessionManager sessions) {
        this.serverIdentity = serverIdentity;
        this.sessions = sessions;
    }

    /**
     * Create a new group
     * @param req of the new group request
     * @return response to send to client
     */
    public BatchProtocolResponse createGroup(CreateGroupRequest req) {
        List<MinecraftPlayer> players = sessions.findPlayers(req.identity, req.players);
        MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        Group group = Group.newGroup(UUID.randomUUID(), player, Location.UNKNOWN, players);
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        synchronized (group.id) {
            updateState(group);
            List<Member> members = group.members.values().stream()
                    .filter(member -> member.membershipRole.equals(Group.MembershipRole.MEMBER))
                    .collect(Collectors.toList());
            response = response.concat(createGroupMembershipRequests(req.identity, group, members));
            response = response.add(req.identity, new CreateGroupResponse(serverIdentity, group));
        }
        return response;
    }

    /**
     * Accept a membership request
     * @param req of the new group request
     * @return response to send the client
     */
    public BatchProtocolResponse acceptMembership(JoinGroupRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return BatchProtocolResponse.one(req.identity, new JoinGroupResponse(serverIdentity, null, null));
        }
        MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        synchronized (group.id) {
            Group.MembershipState state = req.state;
            group = group.updateMembershipState(player, state);
            updateState(group);
            BatchProtocolResponse response = BatchProtocolResponse.one(req.identity, new JoinGroupResponse(serverIdentity, group, player));
            response = response.concat(sendUpdatesToMembers(group, Group.MembershipState.ACCEPTED, ((identity, updatedGroup, updatedMember) -> new JoinGroupResponse(serverIdentity, updatedGroup, player))));
            return response;
        }
    }

    /**
     * Leave the group
     * @param req to leave the group
     * @return response to client
     */
    public BatchProtocolResponse leaveGroup(LeaveGroupRequest req) {
        MinecraftPlayer sender = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            LOGGER.log(Level.INFO, sender + " was not a member of the group " + req.groupId);
            return BatchProtocolResponse.one(req.identity, new LeaveGroupResponse(serverIdentity, null, null));
        }
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        synchronized (group.id) {
            response = response.concat(sendUpdatesToMembers(group, null, (identity, group1, member) -> new LeaveGroupResponse(serverIdentity, group1.id, sender)));
            group = group.removeMember(sender);
            updateState(group);
        }
        LOGGER.log(Level.INFO, "Group count " + groupsById.size());
        return response;
    }

    /**
     * Removes player from all groups
     * @param player to remove
     */
    public BatchProtocolResponse removeUserFromAllGroups(MinecraftPlayer player) {
        List<Group> groups = findGroupsForPlayer(player);
        BatchProtocolResponse response = new BatchProtocolResponse(null);
        for (Group group : groups) {
            synchronized (group.id) {
                group = group.updateMembershipState(player, Group.MembershipState.DECLINED);
                updateState(group);
                response = response.concat(sendUpdatesToMembers(group, null, (identity, updatedGroup, updatedMember) -> new LeaveGroupResponse(serverIdentity, updatedGroup.id, player)));
            }
        }
        LOGGER.log(Level.INFO, "Removed user " + player + " from all groups");
        return response;
    }

    /**
     * Invite user to a group
     * @param req request
     */
    public BatchProtocolResponse invite(GroupInviteRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return new BatchProtocolResponse(serverIdentity);
        }
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        synchronized (group.id) {
            MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
            Member requester = group.members.get(player);
            if (requester == null) {
                LOGGER.log(Level.INFO, player + " is not a member of the group "  + group.id);
                return new BatchProtocolResponse(serverIdentity);
            }
            if (requester.membershipRole != Group.MembershipRole.OWNER) {
                LOGGER.log(Level.INFO, player + " is not OWNER member of the group "  + group.id);
                return new BatchProtocolResponse(serverIdentity);
            }
            Map<Group, List<Member>> groupToMembers = new HashMap<>();
            List<MinecraftPlayer> players = sessions.findPlayers(req.identity, req.players);
            group = group.addMembers(players, Group.MembershipRole.MEMBER, Group.MembershipState.PENDING, groupToMembers::put);
            updateState(group);
            for (Map.Entry<Group, List<Member>> entry : groupToMembers.entrySet()) {
                response = response.concat(createGroupMembershipRequests(req.identity, entry.getKey(), entry.getValue()));
            }
            return response;
        }
    }

    /**
     * Update the player state
     * @param req to update player position
     * @return response to send to client
     */
    public BatchProtocolResponse updatePosition(UpdateLocationRequest req) {
        MinecraftPlayer owner = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
        List<Group> groups = findGroupsForPlayer(owner);
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
//        for (Group group : groups) {
//            synchronized (group.id) {
//                group = group.updateMemberPosition(owner, req.location);
//                updateState(group);
//                responses = responses.concat(sendUpdatesToMembers(owner, group, Group.MembershipState.ACCEPTED, (identity, group1, member) -> new UpdateLocationResponse(serverIdentity, req.identity, owner, req.location)));
//            }
//        }
        return responses;
    }

    public ProtocolResponse ejectMember(EjectGroupMemberRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return new LeaveGroupResponse(serverIdentity, null, null);
        }
        MinecraftPlayer sender = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
        Optional<Member> playerMemberRecord = group.members.values().stream().filter(member -> member.player.equals(sender) && member.membershipRole.equals(Group.MembershipRole.OWNER)).findFirst();
        if (playerMemberRecord.isEmpty()) {
            return new LeaveGroupResponse(serverIdentity, null, null);
        }
        Optional<Member> memberToRemove = group.members.values().stream().filter(member -> member.player.id.equals(req.player)).findFirst();
        if (memberToRemove.isEmpty()) {
            return new LeaveGroupResponse(serverIdentity, null, null);
        }
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        synchronized (group.id) {
            response = response.concat(sendUpdatesToMembers(group, null, (identity, group1, member) -> new LeaveGroupResponse(serverIdentity, group1.id, memberToRemove.get().player)));
            group = group.removeMember(memberToRemove.get().player);
            updateState(group);
        }
        return response;
    }

    public ProtocolResponse createWaypoint(CreateWaypointRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return new CreateWaypointFailedResponse(serverIdentity, req.groupId, req.waypointName);
        }
        MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
        if (group.members.values().stream().noneMatch(member -> member.player.inServerWith(player))) {
            return new CreateWaypointFailedResponse(serverIdentity, req.groupId, req.waypointName);
        }
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), req.waypointName, req.location);
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        synchronized (group.id) {
            group = group.addWaypoint(waypoint);
            updateState(group);
            responses = responses.concat(sendUpdatesToMembers(group, Group.MembershipState.ACCEPTED, (identity, updatedGroup, member) -> new CreateWaypointSuccessResponse(serverIdentity, updatedGroup.id, waypoint)));
        }
        return responses;
    }

    public ProtocolResponse removeWaypoint(RemoveWaypointRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return new RemoveWaypointFailedResponse(serverIdentity, req.groupId, req.waypointId);
        }
        MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
        if (group.members.values().stream().noneMatch(member -> member.player.inServerWith(player))) {
            return new RemoveWaypointFailedResponse(serverIdentity, req.groupId, req.waypointId);
        }
        Waypoint waypoint = group.waypoints.get(req.waypointId);
        if (waypoint == null) {
            return new RemoveWaypointFailedResponse(serverIdentity, req.groupId, req.waypointId);
        }
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        synchronized (group.id) {
            group = group.removeWaypoint(req.waypointId);
            if (group != null) {
                updateState(group);
                responses = responses.concat(sendUpdatesToMembers(group, Group.MembershipState.ACCEPTED, (identity, group1, member) -> new RemoveWaypointResponse.RemoveWaypointSuccessResponse(serverIdentity, group1.id, waypoint.id)));
            } else {
                responses = responses.add(req.identity, new RemoveWaypointFailedResponse(serverIdentity, req.groupId, req.waypointId));
            }
        }
        return responses;
    }

    /**
     * Sends membership requests to the group members
     * @param requester whos sending the request
     * @param group the group to invite to
     * @param members members to send requests to. If null, defaults to the full member list.
     */
    private BatchProtocolResponse createGroupMembershipRequests(ClientIdentity requester, Group group, List<Member> members) {
        MinecraftPlayer sender = sessions.findPlayer(requester).orElseThrow(() -> new IllegalStateException("could not find player for " + requester));
        synchronized (group.id) {
            Collection<Member> memberList = members == null ? group.members.values() : members;
            Map<ProtocolResponse, ClientIdentity> responses = memberList.stream()
                    .filter(member -> member.membershipState == Group.MembershipState.PENDING)
                    .map(member -> member.player)
                    .collect(Collectors.toMap(
                            o -> new GroupInviteResponse(serverIdentity, group.id, sender, new ArrayList<>(group.members.keySet())),
                            minecraftPlayer -> sessions.getIdentity(minecraftPlayer).orElseThrow(() -> new IllegalStateException("cannot find identity for " + minecraftPlayer)))
                    );
            return new BatchProtocolResponse(serverIdentity, responses);
        }
    }

    private void updateState(Group group) {
        synchronized (group.id) {
            if (group.members.isEmpty()) {
                LOGGER.log(Level.INFO, "Removed group " + group.id + " as it has no members.");
                groupsById.remove(group.id);
            } else {
                groupsById.put(group.id, group);
            }
        }
    }

    private BatchProtocolResponse sendUpdatesToMembers(Group group, Group.MembershipState withState, MessageCreator messageCreator) {
        synchronized (group.id) {
            final Map<ProtocolResponse, ClientIdentity> responses = new HashMap<>();
            for (Map.Entry<MinecraftPlayer, Member> memberEntry : group.members.entrySet()) {
                MinecraftPlayer player = memberEntry.getKey();
                Member member = memberEntry.getValue();
                if (withState != null && withState != member.membershipState) {
                    continue;
                }
                Optional<ClientIdentity> identity = sessions.getIdentity(player);
                identity.ifPresent(clientIdentity -> {
                    ProtocolResponse resp = messageCreator.create(clientIdentity, group, member);
                    responses.put(resp, clientIdentity);
                });
            }
            return new BatchProtocolResponse(serverIdentity, responses);
        }
    }

    private List<Group> findGroupsForPlayer(MinecraftPlayer player) {
        return groupsById.values().stream().filter(group -> group.members.containsKey(player)).collect(Collectors.toList());
    }

    interface MessageCreator {
        ProtocolResponse create(Identity identity, Group updatedGroup, Member updatedMember);
    }
}
