package team.catgirl.collar.server.services.groups;

import com.google.common.collect.ImmutableList;
import team.catgirl.collar.api.groups.*;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.messaging.SendMessageRequest;
import team.catgirl.collar.protocol.messaging.SendMessageResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.location.NearbyGroups;
import team.catgirl.collar.server.session.SessionManager;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class GroupService {

    private static final Logger LOGGER = Logger.getLogger(GroupService.class.getName());

    private final GroupStore store;
    private final ServerIdentity serverIdentity;
    private final SessionManager sessions;

    public GroupService(GroupStore store, ServerIdentity serverIdentity, SessionManager sessions) {
        this.store = store;
        this.serverIdentity = serverIdentity;
        this.sessions = sessions;
    }

    /**
     * @param groupIds to find
     * @return the list of matching groups
     */
    public Set<Group> findGroups(Set<UUID> groupIds) {
        return store.findGroups(groupIds).collect(Collectors.toSet());
    }

    /**
     * Create a new group
     * @param req of the new group request
     * @return response to send to client
     */
    public BatchProtocolResponse createGroup(CreateGroupRequest req) {
        if (req.type == GroupType.NEARBY) {
            throw new IllegalStateException("clients cannot create nearby groups");
        }
        if (store.findGroup(req.groupId).isPresent()) {
            throw new IllegalStateException("Group " + req.groupId + " already exists");
        }
        List<Player> players = sessions.findPlayers(req.identity, req.players);
        Player player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        Group group = Group.newGroup(req.groupId, req.name, req.type, player, players);
        List<Member> members = group.members.values().stream()
                .filter(member -> member.membershipRole.equals(MembershipRole.MEMBER))
                .collect(Collectors.toList());
        response.concat(createGroupMembershipRequests(req.identity, group, members));
        response.add(req.identity, new CreateGroupResponse(serverIdentity, group));
        store.upsert(group);
        return response;
    }

    /**
     * Set the player as online
     * @param identity of the joining player
     * @param player the joining player
     * @return responses to send
     */
    public ProtocolResponse playerIsOnline(ClientIdentity identity, Player player) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroupsContaining(player).forEach(group -> {
            group = group.updatePlayer(player);
            response.add(identity, new JoinGroupResponse(serverIdentity, group.id, identity, player.minecraftPlayer, null));
            // Let everyone else in the group know that this identity has come online
            Group finalGroup = group;
            BatchProtocolResponse updates = createMemberMessages(
                    group,
                    member -> member.membershipState.equals(MembershipState.ACCEPTED),
                    ((memberIdentity, memberPlayer, updatedMember) -> new JoinGroupResponse(serverIdentity, finalGroup.id, identity, memberPlayer, null)));
            response.concat(updates);
            updateState(group);
        });
        return response;
    }

    /**
     * Set the player as offline
     * @param identity of the joining player
     * @param player the joining player
     * @return responses to send
     */
    public ProtocolResponse playerIsOffline(ClientIdentity identity, Player player) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroupsContaining(player).forEach(group -> {
            group = group.updatePlayer(player);
            // Let everyone else in the group know that this identity has gone offline
            Group finalGroup = group;
            BatchProtocolResponse updates = createMemberMessages(
                    group,
                    member -> member.membershipState.equals(MembershipState.ACCEPTED),
                    ((memberIdentity, memberPlayer, updatedMember) -> new GroupMemberOfflineResponse(serverIdentity, finalGroup.id, identity)));
            response.concat(updates);
            updateState(group);
        });
        return response;
    }

    /**
     * Accept a membership request
     * @param req of the new group request
     * @return response to send the client
     */
    public BatchProtocolResponse acceptMembership(JoinGroupRequest req) {
        Player sendingPlayer = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroup(req.groupId).ifPresent(group -> {
            MembershipState state = req.state;
            group = group.updateMembershipState(sendingPlayer, state);
            MembershipRole role = group.getRole(sendingPlayer);
            store.updateMember(group.id, sendingPlayer.profile, role, state);
            // Send a response back to the player accepting membership, with the distribution keys
            response.add(req.identity, new JoinGroupResponse(serverIdentity, group.id, req.identity, sendingPlayer.minecraftPlayer, req.keys));
            // Let everyone else in the group know that this identity has accepted
            Group finalGroup = group;
            BatchProtocolResponse updates = createMemberMessages(
                    group,
                    member -> member.membershipState.equals(MembershipState.ACCEPTED),
                    ((identity, player, updatedMember) -> new JoinGroupResponse(serverIdentity, finalGroup.id, req.identity, player, req.keys)));
            response.concat(updates);
            updateState(group);
        });
        return response;
    }

    /**
     * Leave the group
     * @param req to leave the group
     * @return response to client
     */
    public BatchProtocolResponse leaveGroup(LeaveGroupRequest req) {
        Player sender = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroup(req.groupId).ifPresent(group -> {
            Group finalGroup = group;
            response.concat(createMemberMessages(group, member -> true, (identity, player, member) -> new LeaveGroupResponse(serverIdentity, finalGroup.id, req.identity, sender.minecraftPlayer)));
            group = group.removeMember(sender);
            store.removeMember(group.id, sender.profile);
            updateState(group);
        });
        return response;
    }

    /**
     * Invite user to a group
     * @param req request
     */
    public BatchProtocolResponse invite(GroupInviteRequest req) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroup(req.groupId).ifPresent(group -> {
            Player player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
            Member requester = group.members.get(player);
            if (requester == null) {
                LOGGER.log(Level.INFO, player + " is not a member of the group "  + group.id);
                return;
            }
            if (requester.membershipRole != MembershipRole.OWNER) {
                LOGGER.log(Level.INFO, player + " is not OWNER member of the group "  + group.id);
                return;
            }
            Map<Group, List<Member>> groupToMembers = new HashMap<>();
            List<Player> players = sessions.findPlayers(req.identity, req.players);
            group = group.addMembers(players, MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
            store.addMembers(group.id, players, MembershipRole.MEMBER, MembershipState.PENDING);
            for (Map.Entry<Group, List<Member>> entry : groupToMembers.entrySet()) {
                response.concat(createGroupMembershipRequests(req.identity, entry.getKey(), entry.getValue()));
            }
            updateState(group);
        });
        return response;
    }

    public ProtocolResponse ejectMember(EjectGroupMemberRequest req) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroup(req.groupId).ifPresent(group -> {
            Player sender = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
            Optional<Member> playerMemberRecord = group.members.values().stream().filter(member -> member.player.equals(sender) && member.membershipRole.equals(MembershipRole.OWNER)).findFirst();
            if (playerMemberRecord.isEmpty()) {
                return;
            }
            Optional<Member> memberToRemove = group.members.values().stream().filter(member -> member.player.minecraftPlayer.id.equals(req.player)).findFirst();
            if (memberToRemove.isEmpty()) {
                return;
            }
            Player playerToRemove = memberToRemove.get().player;
            Optional<ClientIdentity> identityToRemove = sessions.getIdentity(playerToRemove);
            if (identityToRemove.isEmpty()) {
                return;
            }
            Group finalGroup = group;
            response.concat(createMemberMessages(group, member -> true, (identity, player, member) -> new LeaveGroupResponse(serverIdentity, finalGroup.id, identityToRemove.get(), playerToRemove.minecraftPlayer)));
            group = group.removeMember(playerToRemove);
            store.removeMember(group.id, playerToRemove.profile);
            updateState(group);
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
        store.findGroup(req.group).ifPresent(group -> {
            Player sendingPlayer = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity.id()));
            BatchProtocolResponse updates = createMemberMessages(
                    group,
                    member -> member.membershipState.equals(MembershipState.ACCEPTED) && !member.player.equals(sendingPlayer),
                    (identity, player, member) -> new SendMessageResponse(serverIdentity, req.identity, group.id, player, req.message)
            );
            response.concat(updates);
        });
        return response;
    }

    /**
     * Sends the group keys of the client receiving the {@link JoinGroupResponse} back to the client that joined
     * @param req from the client receiving {@link JoinGroupResponse}
     * @return AcknowledgedGroupJoinedResponse back to the client who joined
     */
    public ProtocolResponse acknowledgeJoin(AcknowledgedGroupJoinedRequest req) {
        return store.findGroup(req.group).map(group -> {
            Player player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException(req.identity + " could not be found in the session"));
            if (!group.containsPlayer(player)) {
                throw new IllegalStateException(player + " is not a member of group " + group.id);
            }
            return BatchProtocolResponse.one(req.recipient, new AcknowledgedGroupJoinedResponse(serverIdentity, req.identity, player.minecraftPlayer, group, req.keys));
        }).orElse(new BatchProtocolResponse(serverIdentity));
    }

    public BatchProtocolResponse updateNearbyGroups(NearbyGroups.Result result) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        result.add.forEach((uuid, nearbyGroup) -> {
            String server = nearbyGroup.players.stream().findFirst().orElseThrow(() -> new IllegalStateException("could not find any players")).minecraftPlayer.server;
            store.findGroup(uuid).ifPresent(group -> {
                group = new Group(uuid, null, GroupType.NEARBY, server, Map.of());
                Map<Group, List<Member>> groupToMembers = new HashMap<>();
                group = group.addMembers(ImmutableList.copyOf(nearbyGroup.players), MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
                for (Map.Entry<Group, List<Member>> memberEntry : groupToMembers.entrySet()) {
                    response.concat(createGroupMembershipRequests(null, memberEntry.getKey(), memberEntry.getValue()));
                }
                updateState(group);
            });
        });

        // TODO: delay group removal by 1 minute
        result.remove.forEach((uuid, nearbyGroup) -> store.findGroup(uuid).ifPresent(group -> {
            for (Player player : nearbyGroup.players) {
                sessions.getIdentity(player).ifPresent(identity -> response.add(identity, new LeaveGroupResponse(serverIdentity, uuid, null, player.minecraftPlayer)));
                group = group.removeMember(player);
            }
            updateState(group);
        }));
        return response;
    }

    /**
     * Sends membership requests to the group members
     * @param requester who's sending the request
     * @param group the group to invite to
     * @param members members to send requests to. If null, defaults to the full member list.
     */
    private BatchProtocolResponse createGroupMembershipRequests(ClientIdentity requester, Group group, List<Member> members) {
        Player sender = sessions.findPlayer(requester).orElse(null);
        Collection<Member> memberList = members == null ? group.members.values() : members;
        Map<ProtocolResponse, ClientIdentity> responses = memberList.stream()
                .filter(member -> member.player == null || member.membershipState == MembershipState.PENDING)
                .map(member -> member.player)
                .collect(Collectors.toMap(
                        o -> new GroupInviteResponse(serverIdentity, group.id, group.type, sender == null ? null : sender.minecraftPlayer, new ArrayList<>(group.members.keySet().stream().map(player -> player.minecraftPlayer).collect(Collectors.toList()))),
                        player -> sessions.getIdentity(player).orElseThrow(() -> new IllegalStateException("cannot find identity for " + player)))
                );
        return new BatchProtocolResponse(serverIdentity, responses);
    }

    private void updateState(Group group) {
        if (group != null && group.members.isEmpty()) {
            LOGGER.log(Level.INFO, "Removed group " + group.id + " as it has no members.");
            store.delete(group.id);
        }
    }

    public BatchProtocolResponse createMemberMessages(Group group, Predicate<Member> filter, MessageCreator messageCreator) {
        final Map<ProtocolResponse, ClientIdentity> responses = new HashMap<>();
        for (Map.Entry<Player, Member> memberEntry : group.members.entrySet()) {
            Player player = memberEntry.getKey();
            Member member = memberEntry.getValue();
            if (!filter.test(member) && player.minecraftPlayer != null) {
                continue;
            }
            sessions.getIdentity(player).ifPresent(clientIdentity -> {
                ProtocolResponse resp = messageCreator.create(clientIdentity, player.minecraftPlayer, member);
                responses.put(resp, clientIdentity);
            });
        }
        return new BatchProtocolResponse(serverIdentity, responses);
    }

    public Optional<Group> findGroup(UUID groupId) {
        return store.findGroup(groupId);
    }

    public interface MessageCreator {
        ProtocolResponse create(ClientIdentity identity, MinecraftPlayer player, Member updatedMember);
    }
}
