package team.catgirl.collar.server.services.groups;

import com.google.common.collect.ImmutableList;
import team.catgirl.collar.api.friends.Status;
import team.catgirl.collar.api.groups.*;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.messaging.SendMessageRequest;
import team.catgirl.collar.protocol.messaging.SendMessageResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.location.NearbyGroups;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.services.profiles.ProfileService.GetProfileRequest;
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
    private final ProfileService profiles;
    private final SessionManager sessions;

    public GroupService(GroupStore store, ServerIdentity serverIdentity, ProfileService profiles, SessionManager sessions) {
        this.store = store;
        this.serverIdentity = serverIdentity;
        this.profiles = profiles;
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
        List<MemberSource> players = sessions.findPlayers(req.identity, req.players).stream().map(player -> {
            PublicProfile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(player.profile)).profile.toPublic();
            return new MemberSource(player, profile);
        }).collect(Collectors.toList());
        Player player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        PublicProfile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(player.profile)).profile.toPublic();
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        Group group = Group.newGroup(req.groupId, req.name, req.type, new MemberSource(player, profile), players);
        List<Member> members = group.members.stream()
                .filter(member -> member.membershipRole.equals(MembershipRole.MEMBER))
                .collect(Collectors.toList());
        response.concat(createGroupMembershipRequests(req.identity, group, members));
        response.add(req.identity, new CreateGroupResponse(serverIdentity, group));
        store.upsert(group);
        return response;
    }

    /**
     * Delete a group
     * @param req of the delete group request
     * @return response to send to client
     */
    public ProtocolResponse delete(DeleteGroupRequest req) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroup(req.group).ifPresent(group -> {
            Player currentPlayer = this.sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
            if (group.getRole(currentPlayer) != MembershipRole.OWNER) {
                throw new IllegalStateException(req.identity + " is not owner of group " + group.id);
            }
            response.concat(createMemberMessages(group, member -> true, (identity, player, updatedMember) -> new LeaveGroupResponse(serverIdentity, group.id, null, null)));
            store.delete(group.id);
        });
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
            response.add(identity, new RejoinGroupResponse(serverIdentity, group.id));
        });
        return response;
    }

    /**
     * Set the player as offline
     * @param player the joining player
     * @return responses to send
     */
    public ProtocolResponse playerIsOffline(Player player) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroupsContaining(player).forEach(group -> {
            PublicProfile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(player.profile)).profile.toPublic();
            group = group.updatePlayer(new MemberSource(player, profile));
            // Let everyone else in the group know that this identity has gone offline
            Group finalGroup = group;
            BatchProtocolResponse updates = createMemberMessages(
                    group,
                    member -> member.membershipState.equals(MembershipState.ACCEPTED),
                    ((memberIdentity, memberPlayer, updatedMember) -> {
                        // As they are not playing any more, we need to clear their minecraft player and send it to all members
                        Player clearedPlayer = new Player(player.profile, null);
                        return new UpdateGroupMemberResponse(serverIdentity, finalGroup.id, clearedPlayer, profile, Status.OFFLINE, null);
                    }));
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
            MembershipRole role = group.getRole(sendingPlayer);
            group = store.updateMember(group.id, sendingPlayer.profile, role, state).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
            // Send a response back to the player accepting membership, with the distribution keys
            response.add(req.identity, new JoinGroupResponse(serverIdentity, group.id, req.identity, sendingPlayer, req.keys));
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
            response.concat(createMemberMessages(group, member -> true, (identity, player, member) -> new LeaveGroupResponse(serverIdentity, finalGroup.id, req.identity, sender)));
            group = store.removeMember(group.id, sender.profile).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
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
            Member requester = group.members.stream().filter(member -> member.player.equals(player)).findFirst().orElseThrow(() -> new IllegalStateException(player + " is not a member of the group "  + req.groupId));
            if (requester.membershipRole != MembershipRole.OWNER) {
                LOGGER.log(Level.INFO, player + " is not OWNER member of the group "  + group.id);
                return;
            }
            Map<Group, List<Member>> groupToMembers = new HashMap<>();
            List<MemberSource> players = sessions.findPlayers(req.identity, req.players).stream().map(thePlayer -> {
                PublicProfile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(thePlayer.profile)).profile.toPublic();
                return new MemberSource(thePlayer, profile);
            }).collect(Collectors.toList());
            // TODO: replace line below with a method that can do the diff of existing players and new players invited
            group = group.addMembers(players, MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
            group = store.addMembers(group.id, players, MembershipRole.MEMBER, MembershipState.PENDING).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
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
            Optional<Member> playerMemberRecord = group.members.stream().filter(member -> member.player.equals(sender) && member.membershipRole.equals(MembershipRole.OWNER)).findFirst();
            if (playerMemberRecord.isEmpty()) {
                return;
            }
            Optional<Member> memberToRemove = group.members.stream().filter(member -> member.player.minecraftPlayer.id.equals(req.player)).findFirst();
            if (memberToRemove.isEmpty()) {
                return;
            }
            Player playerToRemove = memberToRemove.get().player;
            Optional<ClientIdentity> identityToRemove = sessions.getIdentity(playerToRemove);
            if (identityToRemove.isEmpty()) {
                return;
            }
            Group finalGroup = group;
            response.concat(createMemberMessages(group, member -> true, (identity, player, member) -> new LeaveGroupResponse(serverIdentity, finalGroup.id, identityToRemove.get(), playerToRemove)));
            group = store.removeMember(group.id, playerToRemove.profile).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
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
            return BatchProtocolResponse.one(req.recipient, new AcknowledgedGroupJoinedResponse(serverIdentity, req.identity, player, group, req.keys));
        }).orElse(new BatchProtocolResponse(serverIdentity));
    }

    public BatchProtocolResponse updateNearbyGroups(NearbyGroups.Result result) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        result.add.forEach((groupId, nearbyGroup) -> {
            Group group = new Group(groupId, null, GroupType.NEARBY, Set.of());
            Map<Group, List<Member>> groupToMembers = new HashMap<>();
            group = group.addMembers(ImmutableList.copyOf(nearbyGroup.players), MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
            for (Map.Entry<Group, List<Member>> memberEntry : groupToMembers.entrySet()) {
                response.concat(createGroupMembershipRequests(null, memberEntry.getKey(), memberEntry.getValue()));
            }
            store.upsert(group);
        });

        // TODO: delay group removal by 1 minute
        result.remove.forEach((groupId, nearbyGroup) -> store.findGroup(groupId).ifPresent(group -> {
            for (MemberSource source : nearbyGroup.players) {
                sessions.getIdentity(source.player).ifPresent(identity -> response.add(identity, new LeaveGroupResponse(serverIdentity, groupId, null, source.player)));
                group = group.removeMember(source.player);
            }
            store.delete(group.id);
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
        Map<ProtocolResponse, ClientIdentity> responses = members.stream()
                .filter(member -> member.player == null || member.membershipState == MembershipState.PENDING)
                .map(member -> member.player)
                .collect(Collectors.toMap(
                        o -> new GroupInviteResponse(serverIdentity, group.id, group.name, group.type, sender),
                        player -> sessions.getIdentity(player).orElseThrow(() -> new IllegalStateException("cannot find identity for " + player)))
                );
        return new BatchProtocolResponse(serverIdentity, responses);
    }

    public ProtocolResponse transferOwnership(TransferGroupOwnershipRequest req) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroup(req.group).ifPresent(group -> {
            Player currentPlayer = this.sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
            UUID groupId = group.id;
            if (group.getRole(currentPlayer) != MembershipRole.OWNER) {
                throw new IllegalStateException(req.identity + " is not owner of group " + groupId);
            }
            Member newOwner = group.members.stream()
                    .filter(member -> member.player.profile.equals(req.profile)).findFirst()
                    .orElseThrow(() -> new IllegalStateException(req.profile + " is not member of group " + req.group));
            if (newOwner.membershipState == MembershipState.ACCEPTED) {
                group = store.updateMember(groupId, req.profile, MembershipRole.OWNER, MembershipState.ACCEPTED).orElseThrow(() -> new IllegalStateException("cant find group " + groupId));
                // Set the member as owner
                response.concat(createMemberMessages(
                        group,
                        member -> true,
                        (identity, player, updatedMember) -> {
                            PublicProfile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(player.profile)).profile.toPublic();
                            return new UpdateGroupMemberResponse(serverIdentity, groupId, newOwner.player, profile, null, MembershipRole.OWNER);
                        }));

                // Set original owner as member
                group = store.updateMember(groupId, currentPlayer.profile, MembershipRole.MEMBER, MembershipState.ACCEPTED).orElseThrow(() -> new IllegalStateException("cant find group " + groupId));
                response.concat(createMemberMessages(
                        group,
                        member -> true,
                        (identity, player, updatedMember) -> {
                            PublicProfile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(player.profile)).profile.toPublic();
                            return new UpdateGroupMemberResponse(serverIdentity, groupId, currentPlayer, profile, null, MembershipRole.MEMBER);
                        }));
            }
        });
        return response;
    }

    private void updateState(Group group) {
        if (group != null && group.members.isEmpty()) {
            LOGGER.log(Level.INFO, "Removed group " + group.id + " as it has no members.");
            store.delete(group.id);
        }
    }

    public BatchProtocolResponse createMemberMessages(Group group, Predicate<Member> filter, MessageCreator messageCreator) {
        final Map<ProtocolResponse, ClientIdentity> responses = new HashMap<>();
        for (Member member : group.members) {
            if (!filter.test(member) && member.player != null) {
                continue;
            }
            sessions.getIdentity(member.player).ifPresent(clientIdentity -> {
                ProtocolResponse resp = messageCreator.create(clientIdentity, member.player, member);
                responses.put(resp, clientIdentity);
            });
        }
        return new BatchProtocolResponse(serverIdentity, responses);
    }

    public Optional<Group> findGroup(UUID groupId) {
        return store.findGroup(groupId);
    }

    public interface MessageCreator {
        ProtocolResponse create(ClientIdentity identity, Player player, Member updatedMember);
    }
}
