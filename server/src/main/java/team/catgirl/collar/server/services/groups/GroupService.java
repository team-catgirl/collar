package team.catgirl.collar.server.services.groups;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import team.catgirl.collar.api.friends.Status;
import team.catgirl.collar.api.groups.*;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.messaging.SendMessageRequest;
import team.catgirl.collar.protocol.messaging.SendMessageResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.location.NearbyGroups;
import team.catgirl.collar.server.services.profiles.ProfileCache;
import team.catgirl.collar.server.session.SessionManager;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class GroupService {

    private static final Logger LOGGER = Logger.getLogger(GroupService.class.getName());

    private final GroupStore store;
    private final ServerIdentity serverIdentity;
    private final ProfileCache profiles;
    private final SessionManager sessions;

    public GroupService(GroupStore store, ServerIdentity serverIdentity, ProfileCache profiles, SessionManager sessions) {
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
    public Optional<BatchProtocolResponse> createGroup(CreateGroupRequest req) {
        if (req.type == GroupType.NEARBY) {
            throw new IllegalStateException("clients cannot create nearby groups");
        }
        if (store.findGroup(req.groupId).isPresent()) {
            throw new IllegalStateException("Group " + req.groupId + " already exists");
        }

        List<MemberSource> players = sessions.findPlayers(req.identity, req.players).stream().map(player -> {
            PublicProfile profile = profiles.getById(player.profile).orElseThrow(() -> new IllegalStateException("cannot find profile " + player.profile)).toPublic();
            return new MemberSource(player, profile);
        }).collect(Collectors.toList());

        return sessions.findPlayer(req.identity)
                .map(player -> {
                    Profile profile = profiles.getById(player.profile).orElseThrow(() -> new IllegalStateException("cant find player with profile " + player.profile));
                    Group group = Group.newGroup(req.groupId, req.name, req.type, new MemberSource(player, profile.toPublic()), players);
                    List<Member> members = group.members.stream()
                            .filter(member -> member.membershipRole.equals(MembershipRole.MEMBER))
                            .collect(Collectors.toList());
                    store.upsert(group);
                    BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
                    createGroupMembershipRequests(req.identity, group, members).ifPresent(response::concat);
                    response.add(req.identity, new CreateGroupResponse(serverIdentity, group));
                    return response;
                });
    }

    /**
     * Delete a group
     * @param req of the delete group request
     * @return response to send to client
     */
    public Optional<ProtocolResponse> delete(DeleteGroupRequest req) {
        Optional<Group> group = store.findGroup(req.group);
        if (group.isPresent()) {
            Group theGroup = group.get();
            return this.sessions.findPlayer(req.identity).map(player -> {
                if (theGroup.getRole(player) != MembershipRole.OWNER) {
                    throw new IllegalStateException(req.identity + " is not owner of group " + theGroup.id);
                }
                store.delete(theGroup.id);
                return createMemberMessages(theGroup, member -> true, (identity, thePlayer, updatedMember) -> new LeaveGroupResponse(serverIdentity, theGroup.id, null, null));
            });
        }
        return Optional.empty();
    }

    /**
     * Sends responses to rejoin any groups and re-issue invitations
     * @param identity of the joining player
     * @param player the joining player
     * @return responses to send
     */
    public Optional<BatchProtocolResponse> playerIsOnline(ClientIdentity identity, Player player) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroupsContaining(player).forEach(group -> {
            group.findMember(player).ifPresent(member -> {
                if (!member.player.equals(player)) {
                    return;
                }
                switch (member.membershipState) {
                    case DECLINED, PENDING -> response.add(identity, new GroupInviteResponse(serverIdentity, group.id, group.name, group.type, null));
                    case ACCEPTED -> response.add(identity, new RejoinGroupResponse(serverIdentity, group.id));
                    default -> throw new IllegalStateException("Unexpected value: " + member.membershipState);
                }
            });
        });
        return Optional.of(response);
    }

    /**
     * Set the player as offline
     * @param player the joining player
     * @return responses to send
     */
    public Optional<ProtocolResponse> playerIsOffline(Player player) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        store.findGroupsContaining(player).forEach(group -> {
            PublicProfile profile = profiles.getById(player.profile).orElseThrow(() -> new IllegalStateException("could not load profile " + player.profile)).toPublic();
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
        return Optional.of(response);
    }

    /**
     * Accept a membership request
     * @param req of the new group request
     * @return response to send the client
     */
    public Optional<BatchProtocolResponse> acceptMembership(JoinGroupRequest req) {
        Optional<Player> sendingPlayer = sessions.findPlayer(req.identity);
        if (sendingPlayer.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.groupId).map(group -> {
            BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
            MembershipState state = req.state;
            MembershipRole role = group.getRole(sendingPlayer.get());
            group = store.updateMember(group.id, sendingPlayer.get().profile, role, state).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
            // Let everyone in the group (including sender) know that they have accepted
            Group finalGroup = group;
            BatchProtocolResponse updates = createMemberMessages(
                    group,
                    member -> member.membershipState.equals(MembershipState.ACCEPTED),
                    ((identity, player, updatedMember) -> new JoinGroupResponse(serverIdentity, finalGroup.id, req.identity, player, req.keys)));
            response.concat(updates);
            updateState(group);
            return response;
        });
    }

    /**
     * Leave the group
     * @param req to leave the group
     * @return response to client
     */
    public Optional<BatchProtocolResponse> leaveGroup(LeaveGroupRequest req) {
        Optional<Player> sender = sessions.findPlayer(req.identity);
        if (sender.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.groupId).map(group -> {
            Group finalGroup = group;
            BatchProtocolResponse response = createMemberMessages(group, member -> true, (identity, player, member) -> new LeaveGroupResponse(serverIdentity, finalGroup.id, req.identity, sender.get()));
            group = store.removeMember(group.id, sender.get().profile).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
            updateState(group);
            return response;
        });
    }

    /**
     * Invite user to a group
     * @param req request
     */
    public Optional<BatchProtocolResponse> invite(GroupInviteRequest req) {
        Optional<Player> player = sessions.findPlayer(req.identity);
        if (player.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.groupId).map(group -> {
            group.members.stream()
                    .filter(member -> member.player.equals(player.get())).findFirst()
                    .filter(member -> member.membershipRole == MembershipRole.OWNER)
                    .orElseThrow(() -> new IllegalStateException("requester is not owner of group"));
            Map<Group, List<Member>> groupToMembers = new HashMap<>();
            List<MemberSource> players = sessions.findPlayers(req.identity, req.players).stream().map(thePlayer -> {
                PublicProfile profile = profiles.getById(thePlayer.profile).orElseThrow(() -> new IllegalStateException("cannot find profile " + thePlayer.profile)).toPublic();
                return new MemberSource(thePlayer, profile);
            }).collect(Collectors.toList());
            // TODO: replace line below with a method that can do the diff of existing players and new players invited
            BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
            group = group.addMembers(players, MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
            group = store.addMembers(group.id, players, MembershipRole.MEMBER, MembershipState.PENDING).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
            for (Map.Entry<Group, List<Member>> entry : groupToMembers.entrySet()) {
                createGroupMembershipRequests(req.identity, entry.getKey(), entry.getValue()).ifPresent(response::concat);
            }
            updateState(group);
            return response;
        });
    }

    public Optional<BatchProtocolResponse> ejectMember(EjectGroupMemberRequest req) {
        Optional<Player> player = sessions.findPlayer(req.identity);
        if (player.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.groupId).map(group -> {
            Optional<Member> playerMemberRecord = group.members.stream().filter(member -> member.player.equals(player.get()) && member.membershipRole.equals(MembershipRole.OWNER)).findFirst();
            if (playerMemberRecord.isEmpty()) {
                return null;
            }
            Optional<Member> memberToRemove = group.members.stream().filter(member -> member.player.minecraftPlayer.id.equals(req.player)).findFirst();
            if (memberToRemove.isEmpty()) {
                return null;
            }
            Player playerToRemove = memberToRemove.get().player;
            Optional<ClientIdentity> identityToRemove = sessions.getIdentity(playerToRemove);
            if (identityToRemove.isEmpty()) {
                return null;
            }
            BatchProtocolResponse response = createMemberMessages(group, member -> true, (identity, thePlayer, member) -> new LeaveGroupResponse(serverIdentity, req.groupId, identityToRemove.get(), playerToRemove));
            group = store.removeMember(group.id, playerToRemove.profile).orElseThrow(() -> new IllegalStateException("could not reload group " + req.groupId));
            updateState(group);
            return response;
        });
    }

    /**
     * Creates messages to be sent to all ACCEPTED members of the group it is addressed to
     * @param req of the message
     * @return responses
     */
    public Optional<ProtocolResponse> createMessages(SendMessageRequest req) {
        Optional<Player> player = sessions.findPlayer(req.identity);
        if (player.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.group).map(group -> createMemberMessages(
                group,
                member -> member.membershipState.equals(MembershipState.ACCEPTED) && !member.player.equals(player.get()),
                (identity, thePlayer, member) -> new SendMessageResponse(serverIdentity, req.identity, group.id, thePlayer, req.message))
        );
    }

    /**
     * Sends the group keys of the client receiving the {@link JoinGroupResponse} back to the client that joined
     * @param req from the client receiving {@link JoinGroupResponse}
     * @return AcknowledgedGroupJoinedResponse back to the client who joined
     */
    public Optional<ProtocolResponse> acknowledgeJoin(AcknowledgedGroupJoinedRequest req) {
        return store.findGroup(req.group).map(group -> {
            Player player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException(req.identity + " could not be found in the session"));
            if (!group.containsPlayer(player)) {
                throw new IllegalStateException(player + " is not a member of group " + group.id);
            }
            return BatchProtocolResponse.one(req.recipient, new AcknowledgedGroupJoinedResponse(serverIdentity, req.identity, player, group, req.keys));
        });
    }

    public Optional<BatchProtocolResponse> updateNearbyGroups(NearbyGroups.Result result) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        result.add.forEach((groupId, nearbyGroup) -> {
            Group group = new Group(groupId, null, GroupType.NEARBY, Set.of());
            Map<Group, List<Member>> groupToMembers = new HashMap<>();
            group = group.addMembers(ImmutableList.copyOf(nearbyGroup.players), MembershipRole.MEMBER, MembershipState.PENDING, groupToMembers::put);
            for (Map.Entry<Group, List<Member>> memberEntry : groupToMembers.entrySet()) {
                createGroupMembershipRequests(null, memberEntry.getKey(), memberEntry.getValue()).ifPresent(theResponse -> response.concat(response));
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
        return response.optional();
    }

    /**
     * Sends membership requests to the group members
     * @param requester who's sending the request
     * @param group the group to invite to
     * @param members members to send requests to. If null, defaults to the full member list.
     */
    private Optional<BatchProtocolResponse> createGroupMembershipRequests(ClientIdentity requester, Group group, List<Member> members) {
        Optional<Player> sender = sessions.findPlayer(requester);
        if (sender.isEmpty()) {
            return Optional.empty();
        }
        Map<ProtocolResponse, ClientIdentity> responses = members.stream()
                .filter(member -> member.player == null || member.membershipState == MembershipState.PENDING)
                .map(member -> member.player)
                .collect(Collectors.toMap(
                        o -> new GroupInviteResponse(serverIdentity, group.id, group.name, group.type, sender.get()),
                        player -> sessions.getIdentity(player).orElseThrow(() -> new IllegalStateException("cannot find identity for " + player)))
                );
        return new BatchProtocolResponse(serverIdentity, responses).optional();
    }

    public Optional<ProtocolResponse> transferOwnership(TransferGroupOwnershipRequest req) {
        Optional<Player> currentPlayer = sessions.findPlayer(req.identity);
        if (currentPlayer.isEmpty()) {
            return Optional.empty();
        }
        return store.findGroup(req.group).map(group -> {
            UUID groupId = group.id;
            if (group.getRole(currentPlayer.get()) != MembershipRole.OWNER) {
                throw new IllegalStateException(req.identity + " is not owner of group " + groupId);
            }
            Member newOwner = group.members.stream()
                    .filter(member -> member.player.profile.equals(req.profile)).findFirst()
                    .orElseThrow(() -> new IllegalStateException(req.profile + " is not member of group " + req.group));

            if (newOwner.membershipState != MembershipState.ACCEPTED) {
                return null;
            }

            group = store.updateMember(groupId, req.profile, MembershipRole.OWNER, MembershipState.ACCEPTED).orElseThrow(() -> new IllegalStateException("cant find group " + groupId));
            // Set the member as owner
            BatchProtocolResponse removeOldOwnerMessages = createMemberMessages(
                    group,
                    member -> true,
                    (identity, player, updatedMember) -> {
                        PublicProfile profile = profiles.getById(player.profile).orElseThrow(() -> new IllegalStateException("cannot find profile " + player.profile)).toPublic();
                        return new UpdateGroupMemberResponse(serverIdentity, groupId, newOwner.player, profile, null, MembershipRole.OWNER);
                    }
            );

            // Set original owner as member
            group = store.updateMember(groupId, currentPlayer.get().profile, MembershipRole.MEMBER, MembershipState.ACCEPTED).orElseThrow(() -> new IllegalStateException("cant find group " + groupId));
            BatchProtocolResponse addNewOwnerMessages = createMemberMessages(
                    group,
                    member -> true,
                    (identity, player, updatedMember) -> {
                        PublicProfile profile = profiles.getById(player.profile).orElseThrow(() -> new IllegalStateException("cannot find profile " + player.profile)).toPublic();
                        return new UpdateGroupMemberResponse(serverIdentity, groupId, currentPlayer.get(), profile, null, MembershipRole.MEMBER);
                    });

            return removeOldOwnerMessages.concat(addNewOwnerMessages);
        });
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
