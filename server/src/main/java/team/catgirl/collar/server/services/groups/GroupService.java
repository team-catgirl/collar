package team.catgirl.collar.server.services.groups;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.GroupType;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.api.groups.Group.MembershipState;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.messaging.SendMessageRequest;
import team.catgirl.collar.protocol.messaging.SendMessageResponse;
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointFailedResponse;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse.CreateWaypointSuccessResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse.RemoveWaypointFailedResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.location.NearbyGroup;
import team.catgirl.collar.server.services.location.NearbyGroups;
import team.catgirl.collar.server.session.SessionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class GroupService {

    private static final Logger LOGGER = Logger.getLogger(GroupService.class.getName());

    private final ServerIdentity serverIdentity;
    private final SessionManager sessions;

    private final ConcurrentMap<UUID, Group> groupsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> nearbyHashToGroupId = new ConcurrentHashMap<>();

    public GroupService(ServerIdentity serverIdentity, SessionManager sessions) {
        this.serverIdentity = serverIdentity;
        this.sessions = sessions;
    }

    /**
     * @param groupIds to find
     * @return the list of matching groups
     */
    public List<Group> findGroups(List<UUID> groupIds) {
        return groupsById.entrySet().stream()
                .filter(entry -> groupIds.contains(entry.getKey()))
                .map(Map.Entry::getValue).collect(Collectors.toList());
    }

    /**
     * Create a new group
     * @param req of the new group request
     * @return response to send to client
     */
    public BatchProtocolResponse createGroup(CreateGroupRequest req) {
        List<MinecraftPlayer> players = sessions.findPlayers(req.identity, req.players);
        MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        groupsById.compute(req.groupId, (uuid, group) -> {
            if (group != null) {
                throw new IllegalStateException("Group with id " + req.groupId + " already exists");
            }
            group = Group.newGroup(req.groupId, GroupType.PLAYER, player, Location.UNKNOWN, players);
            List<Member> members = group.members.values().stream()
                    .filter(member -> member.membershipRole.equals(Group.MembershipRole.MEMBER))
                    .collect(Collectors.toList());
            response.concat(createGroupMembershipRequests(req.identity, group, members));
            response.add(req.identity, new CreateGroupResponse(serverIdentity, group));
            return checkState(group);
        });
        return response;
    }

    /**
     * Accept a membership request
     * @param req of the new group request
     * @return response to send the client
     */
    public BatchProtocolResponse acceptMembership(JoinGroupRequest req) {
        MinecraftPlayer sendingPlayer = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        groupsById.compute(req.groupId, (uuid, group) -> {
            if (group == null) {
                return null;
            }
            MembershipState state = req.state;
            group = group.updateMembershipState(sendingPlayer, state);
            // Send a response back to the player accepting membership, with the distribution keys
            response.add(req.identity, new JoinGroupResponse(serverIdentity, group, req.identity, sendingPlayer, req.keys));
            // Let everyone else in the group know that this identity has accepted
            Group finalGroup = group;
            BatchProtocolResponse updates = sendUpdatesToMembers(
                    group,
                    member -> member.membershipState.equals(MembershipState.ACCEPTED),
                    ((identity, player, updatedMember) -> new JoinGroupResponse(serverIdentity, finalGroup, req.identity, player, req.keys)));
            response.concat(updates);
            return checkState(group);
        });
        return response;
    }

    /**
     * Leave the group
     * @param req to leave the group
     * @return response to client
     */
    public BatchProtocolResponse leaveGroup(LeaveGroupRequest req) {
        MinecraftPlayer sender = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        groupsById.compute(req.groupId, (uuid, group) -> {
            if (group == null) {
                return null;
            }
            Group finalGroup = group;
            response.concat(sendUpdatesToMembers(group, member -> true, (identity, player, member) -> new LeaveGroupResponse(serverIdentity, finalGroup.id, req.identity, sender)));
            group = group.removeMember(sender);
            return checkState(group);
        });
        LOGGER.log(Level.INFO, "Group count " + groupsById.size());
        return response;
    }

    /**
     * Removes player from all groups
     * @param playerToRemove to remove
     */
    public BatchProtocolResponse removeUserFromAllGroups(MinecraftPlayer playerToRemove) {
        List<Group> groups = findGroupsForPlayer(playerToRemove);
        BatchProtocolResponse response = new BatchProtocolResponse(null);
        for (Group group : groups) {
            groupsById.compute(group.id, (uuid, group1) -> {
                group1 = group.updateMembershipState(playerToRemove, MembershipState.DECLINED);
                Group finalGroup = group1;
                response.concat(sendUpdatesToMembers(group1, member -> true, (identity, player, updatedMember) -> new LeaveGroupResponse(serverIdentity, finalGroup.id, identity, player)));
                return checkState(finalGroup);
            });
        }
        LOGGER.log(Level.INFO, "Removed user " + playerToRemove + " from all groups");
        return response;
    }

    /**
     * Invite user to a group
     * @param req request
     */
    public BatchProtocolResponse invite(GroupInviteRequest req) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        groupsById.compute(req.groupId, (uuid, group) -> {
            if (group != null) {
                MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
                Member requester = group.members.get(player);
                if (requester == null) {
                    LOGGER.log(Level.INFO, player + " is not a member of the group "  + group.id);
                    return group;
                }
                if (requester.membershipRole != Group.MembershipRole.OWNER) {
                    LOGGER.log(Level.INFO, player + " is not OWNER member of the group "  + group.id);
                    return group;
                }
                Map<Group, List<Member>> groupToMembers = new HashMap<>();
                List<MinecraftPlayer> players = sessions.findPlayers(req.identity, req.players);
                group = group.addMembers(players, Group.MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
                for (Map.Entry<Group, List<Member>> entry : groupToMembers.entrySet()) {
                    response.concat(createGroupMembershipRequests(req.identity, entry.getKey(), entry.getValue()));
                }
            }
            return checkState(group);
        });
        return response;
    }

    public ProtocolResponse ejectMember(EjectGroupMemberRequest req) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        groupsById.compute(req.groupId, (uuid, group) -> {
            if (group != null) {
                MinecraftPlayer sender = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
                Optional<Member> playerMemberRecord = group.members.values().stream().filter(member -> member.player.equals(sender) && member.membershipRole.equals(Group.MembershipRole.OWNER)).findFirst();
                if (playerMemberRecord.isEmpty()) {
                    return group;
                }
                Optional<Member> memberToRemove = group.members.values().stream().filter(member -> member.player.id.equals(req.player)).findFirst();
                if (memberToRemove.isEmpty()) {
                    return group;
                }
                Optional<ClientIdentity> identityToRemove = sessions.getIdentity(memberToRemove.get().player);
                if (identityToRemove.isEmpty()) {
                    return group;
                }
                Group finalGroup = group;
                response.concat(sendUpdatesToMembers(group, member -> true, (identity, player, member) -> new LeaveGroupResponse(serverIdentity, finalGroup.id, identityToRemove.get(), memberToRemove.get().player)));
                group = group.removeMember(memberToRemove.get().player);
            }
            return checkState(group);
        });
        return response;
    }

    public ProtocolResponse createWaypoint(CreateWaypointRequest req) {
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        groupsById.compute(req.groupId, (uuid, group) -> {
            if (group == null) {
               responses.add(req.identity, new CreateWaypointFailedResponse(serverIdentity, req.groupId, req.waypointName));
               return null;
            }
            MinecraftPlayer sendingPlayer = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
            if (group.members.values().stream().noneMatch(member -> member.player.inServerWith(sendingPlayer))) {
                responses.add(req.identity, new CreateWaypointFailedResponse(serverIdentity, req.groupId, req.waypointName));
                return null;
            }
            Waypoint waypoint = new Waypoint(UUID.randomUUID(), req.waypointName, req.location);
            group = group.addWaypoint(waypoint);
            Group finalGroup = group;
            responses.concat(sendUpdatesToMembers(group, member -> member.membershipState.equals(MembershipState.ACCEPTED), (identity, player, member) -> new CreateWaypointSuccessResponse(serverIdentity, finalGroup.id, waypoint)));
            return checkState(group);
        });
        return responses;
    }

    public ProtocolResponse removeWaypoint(RemoveWaypointRequest req) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        groupsById.compute(req.groupId, (uuid, group) -> {
            if (group == null) {
                response.add(req.identity, new RemoveWaypointFailedResponse(serverIdentity, req.groupId, req.waypointId));
                return null;
            }
            MinecraftPlayer sendingPlayer = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
            if (group.members.values().stream().noneMatch(member -> member.player.inServerWith(sendingPlayer))) {
                response.add(req.identity, new RemoveWaypointFailedResponse(serverIdentity, req.groupId, req.waypointId));
                return group;
            }
            group = group.removeWaypoint(req.waypointId);
            if (group != null) {
                Group finalGroup = group;
                response.concat(sendUpdatesToMembers(group, member -> member.membershipState.equals(MembershipState.ACCEPTED), (identity, player, member) -> new RemoveWaypointResponse.RemoveWaypointSuccessResponse(serverIdentity, finalGroup.id, req.waypointId)));
            } else {
                response.add(req.identity, new RemoveWaypointFailedResponse(serverIdentity, req.groupId, req.waypointId));
            }
            return checkState(group);
        });
        return response;
    }

    /**
     * Creates messages to be sent to all ACCEPTED members of the group it is addressed to
     * @param req of the message
     * @return responses
     */
    public ProtocolResponse createMessages(SendMessageRequest req) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        groupsById.compute(req.group, (uuid, group) -> {
            if (group != null) {
                MinecraftPlayer sendingPlayer = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
                BatchProtocolResponse updates = sendUpdatesToMembers(
                        group,
                        member -> member.membershipState.equals(MembershipState.ACCEPTED) && !member.player.equals(sendingPlayer),
                        (identity, player, member) -> new SendMessageResponse(serverIdentity, req.identity, group.id, player, req.message)
                );
                response.concat(updates);
            }
            return group;
        });
        return response;
    }

    /**
     * Sends the group keys of the client receiving the {@link JoinGroupResponse} back to the client that joined
     * @param req from the client receiving {@link JoinGroupResponse}
     * @return AcknowledgedGroupJoinedResponse back to the client who joined
     */
    public ProtocolResponse acknowledgeJoin(AcknowledgedGroupJoinedRequest req) {
        // Make sure the sender is a member of the group
        Group group = groupsById.get(req.group);
        MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException(req.identity + " could not be found in the session"));
        if (!group.containsPlayer(player)) {
            throw new IllegalStateException(player + " is not a member of group " + group.id);
        }
        return BatchProtocolResponse.one(req.recipient, new AcknowledgedGroupJoinedResponse(serverIdentity, req.identity, req.group, req.keys));
    }

    public BatchProtocolResponse updateNearbyGroups(NearbyGroups.Result result) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);

        result.add.forEach((uuid, nearbyGroup) -> {
            String server = nearbyGroup.players.stream().findFirst().orElseThrow(() -> new IllegalStateException("could not find any players")).server;
            groupsById.compute(uuid, (uuid1, group) -> {
                if (group != null) {
                    throw new IllegalStateException("group " + uuid + " already exists");
                }
                group = new Group(uuid, GroupType.NEARBY, server, Map.of(), Map.of());
                Map<Group, List<Member>> groupToMembers = new HashMap<>();
                group = group.addMembers(ImmutableList.copyOf(nearbyGroup.players), Group.MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
                for (Map.Entry<Group, List<Member>> memberEntry : groupToMembers.entrySet()) {
                    response.concat(createGroupMembershipRequests(null, memberEntry.getKey(), memberEntry.getValue()));
                }
                return checkState(group);
            });
        });

        // TODO: delay group removal by 1 minute
        result.remove.forEach((uuid, nearbyGroup) -> {
            groupsById.compute(uuid, (uuid1, group) -> {
                if (group == null) {
                    throw new IllegalStateException("group " + uuid + " does not exist");
                }
                for (MinecraftPlayer player : nearbyGroup.players) {
                    sessions.getIdentity(player.id).ifPresent(identity -> response.add(identity, new LeaveGroupResponse(serverIdentity, uuid, null, player)));
                    group = group.removeMember(player);
                }
                return checkState(group);
            });
        });
        return response;
    }

    /**
     * Sends membership requests to the group members
     * @param requester whos sending the request
     * @param group the group to invite to
     * @param members members to send requests to. If null, defaults to the full member list.
     */
    private BatchProtocolResponse createGroupMembershipRequests(ClientIdentity requester, Group group, List<Member> members) {
        MinecraftPlayer sender = sessions.findPlayer(requester).orElse(null);
        Collection<Member> memberList = members == null ? group.members.values() : members;
        Map<ProtocolResponse, ClientIdentity> responses = memberList.stream()
                .filter(member -> member.membershipState == MembershipState.PENDING)
                .map(member -> member.player)
                .collect(Collectors.toMap(
                        o -> new GroupInviteResponse(serverIdentity, group.id, group.type, sender, new ArrayList<>(group.members.keySet())),
                        minecraftPlayer -> sessions.getIdentity(minecraftPlayer).orElseThrow(() -> new IllegalStateException("cannot find identity for " + minecraftPlayer)))
                );
        return new BatchProtocolResponse(serverIdentity, responses);
    }

    /**
     * Removes groups with no members from within {@link ConcurrentMap#compute}
     * @param group to check
     * @return group if still valid or null
     */
    private Group checkState(Group group) {
        if (group != null && group.members.isEmpty()) {
            LOGGER.log(Level.INFO, "Removed group " + group.id + " as it has no members.");
            // Remove any nearby hashes associated with this group
            nearbyHashToGroupId.entrySet().stream().filter(entry -> entry.getValue().equals(group.id))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .ifPresent(nearbyHashToGroupId::remove);
            return null;
        }
        return group;
    }

    private BatchProtocolResponse sendUpdatesToMembers(Group group, Predicate<Member> filter, MessageCreator messageCreator) {
        final Map<ProtocolResponse, ClientIdentity> responses = new HashMap<>();
        for (Map.Entry<MinecraftPlayer, Member> memberEntry : group.members.entrySet()) {
            MinecraftPlayer player = memberEntry.getKey();
            Member member = memberEntry.getValue();
            if (!filter.test(member)) {
                continue;
            }
            sessions.getIdentity(player).ifPresent(clientIdentity -> {
                ProtocolResponse resp = messageCreator.create(clientIdentity, player, member);
                responses.put(resp, clientIdentity);
            });
        }
        return new BatchProtocolResponse(serverIdentity, responses);
    }

    private List<Group> findGroupsForPlayer(MinecraftPlayer player) {
        return groupsById.values().stream().filter(group -> group.members.containsKey(player)).collect(Collectors.toList());
    }

    interface MessageCreator {
        ProtocolResponse create(ClientIdentity identity, MinecraftPlayer player, Member updatedMember);
    }
}
