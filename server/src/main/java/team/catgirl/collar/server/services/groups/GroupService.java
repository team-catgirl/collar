package team.catgirl.collar.server.services.groups;

import com.google.common.collect.ImmutableList;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.security.ClientIdentity;
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
        MinecraftPlayer player = sessions.findPlayer(req.identity);
        Group group = Group.newGroup(UUID.randomUUID(), player, null, players);
        synchronized (group.id) {
            return refreshGroupState(group, req.identity, new CreateGroupResponse(serverIdentity, group));
        }
    }

    /**
     * Accept a membership request
     * @param req of the new group request
     * @return response to send the client
     */
    public BatchProtocolResponse acceptMembership(AcceptGroupMembershipRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return BatchProtocolResponse.one(req.identity, new AcceptGroupMembershipResponse(serverIdentity, null));
        }
        MinecraftPlayer owner = sessions.findPlayer(req.identity);
        synchronized (group.id) {
            Group.MembershipState state = req.state;
            group = group.updateMemberState(owner, state);
            return refreshGroupState(group, req.identity, new AcceptGroupMembershipResponse(serverIdentity, group));
        }
    }

    /**
     * Leave the group
     * @param req to leave the group
     * @return response to client
     */
    public BatchProtocolResponse leaveGroup(LeaveGroupRequest req) {
        MinecraftPlayer sender = sessions.findPlayer(req.identity);
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            LOGGER.log(Level.INFO, sender + " was not a member of the group " + req.groupId);
            return BatchProtocolResponse.one(req.identity, new LeaveGroupResponse(serverIdentity, null));
        }
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        synchronized (group.id) {
            group = group.removeMember(sender);
            response = response.concat(refreshGroupState(group, req.identity, new LeaveGroupResponse(serverIdentity, group.id)));
            response = response.concat(createGroupResponses(sender, group, new UpdateGroupMemberPositionResponse(serverIdentity, ImmutableList.of(group))));
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
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        for (Group group : groups) {
            synchronized (group.id) {
                group = group.updateMemberState(player, Group.MembershipState.DECLINED);
                response = response.concat(refreshGroupState(group, sessions.getIdentity(player), null));
            }
        }
        LOGGER.log(Level.INFO, "Removed user " + player + " from all groups");
        return response;
    }

    /**
     * Sends membership requests to the group members
     * @param requester whos sending the request
     * @param group the group to invite to
     * @param members members to send requests to. If null, defaults to the full member list.
     */
    public BatchProtocolResponse createGroupMembershipRequests(ClientIdentity requester, Group group, List<Member> members) {
        MinecraftPlayer sender = sessions.findPlayer(requester);
        synchronized (group.id) {
            Collection<Member> memberList = members == null ? group.members.values() : members;
            Map<ClientIdentity, ProtocolResponse> responseMap = memberList.stream()
                    .filter(member -> member.membershipState == Group.MembershipState.PENDING)
                    .map(member -> member.player)
                    .collect(Collectors.toMap(sessions::getIdentity, o -> new GroupMembershipRequest(serverIdentity, group.id, sender, new ArrayList<>(group.members.keySet()))));
            return new BatchProtocolResponse(serverIdentity, responseMap);
        }
    }

    /**
     * Invite user to a group
     * @param groupInviteRequest request
     */
    public BatchProtocolResponse invite(GroupInviteRequest groupInviteRequest) {
        Group group = groupsById.get(groupInviteRequest.groupId);
        if (group == null) {
            return BatchProtocolResponse.one(groupInviteRequest.identity, new GroupInviteResponse(serverIdentity, null, null));
        }
        List<Member> newMembers = new ArrayList<>();
        synchronized (group.id) {
            MinecraftPlayer player = sessions.findPlayer(groupInviteRequest.identity);
            Member requester = group.members.get(player);
            if (requester == null) {
                LOGGER.log(Level.INFO, player + " is not a member of the group "  + group.id);
                return BatchProtocolResponse.one(groupInviteRequest.identity, new GroupInviteResponse(serverIdentity, null, null));
            }
            if (requester.membershipRole != Group.MembershipRole.OWNER) {
                LOGGER.log(Level.INFO, player + " is not OWNER member of the group "  + group.id);
                return BatchProtocolResponse.one(groupInviteRequest.identity, new GroupInviteResponse(serverIdentity, null, null));
            }
            BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
            Map<Group, List<Member>> groupToMembers = new HashMap<>();

            List<MinecraftPlayer> players = sessions.findPlayers(groupInviteRequest.identity, groupInviteRequest.players);
            group = group.addMembers(players, Group.MembershipRole.MEMBER, Group.MembershipState.PENDING, (newGroup, members) -> {
                groupToMembers.put(newGroup, members);
                newMembers.addAll(members);
            });
            for (Map.Entry<Group, List<Member>> entry : groupToMembers.entrySet()) {
                response = response.concat(createGroupMembershipRequests(groupInviteRequest.identity, entry.getKey(), entry.getValue()));
            }
            return refreshGroupState(group, groupInviteRequest.identity, new GroupInviteResponse(serverIdentity, group.id, newMembers.stream().map(member -> member.player).collect(Collectors.toList())));
        }
    }

    /**
     * Update the player state
     * @param req to update player position
     * @return response to send to client
     */
    public BatchProtocolResponse updatePosition(UpdateGroupMemberPositionRequest req) {
        MinecraftPlayer owner = sessions.findPlayer(req.identity);
        List<Group> groups = findGroupsForPlayer(owner);
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        for (Group group : groups) {
            synchronized (group.id) {
                group = group.updateMemberPosition(owner, req.position);
                responses = responses.concat(refreshGroupState(group, req.identity, null));
                responses = responses.concat(createGroupResponses(owner, group, new UpdateGroupMemberPositionResponse(serverIdentity, groups)));
            }
        }
        return responses.add(req.identity, new UpdateGroupMemberPositionResponse(serverIdentity, findGroupsForPlayer(owner)));
    }

    private List<Group> findGroupsForPlayer(MinecraftPlayer player) {
        return groupsById.values().stream().filter(group -> group.members.containsKey(player)).collect(Collectors.toList());
    }

    private BatchProtocolResponse createGroupResponses(MinecraftPlayer sender, Group group, ProtocolResponse response) {
        synchronized (group.id) {
            Map<ClientIdentity, ProtocolResponse> responses = group.members.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(sender))
                    .filter(entry -> entry.getValue().membershipState == Group.MembershipState.ACCEPTED)
                    .collect(Collectors.toMap(minecraftPlayerMemberEntry -> sessions.getIdentity(minecraftPlayerMemberEntry.getKey()), minecraftPlayerMemberEntry -> response));
            return new BatchProtocolResponse(serverIdentity, responses);
        }
    }

    private BatchProtocolResponse refreshGroupState(Group group, ClientIdentity identity, ProtocolResponse resp) {
        BatchProtocolResponse response = resp != null ? BatchProtocolResponse.one(identity, resp) : new BatchProtocolResponse(serverIdentity);
        synchronized (group.id) {
            if (group.members.isEmpty()) {
                LOGGER.log(Level.INFO, "Removed group " + group.id + " as it has no members.");
                groupsById.remove(group.id);
            } else {
                groupsById.put(group.id, group);
                for (Member member : group.members.values()) {
                    List<Group> groupsForPlayer = findGroupsForPlayer(member.player);
                    if (!groupsForPlayer.isEmpty()) {
                        BatchProtocolResponse groupResponses = createGroupResponses(null, group, new UpdateGroupMemberPositionResponse(serverIdentity, groupsForPlayer));
                        response = groupResponses.concat(groupResponses);
                    }
                }
            }
        }
        return response;
    }
}
