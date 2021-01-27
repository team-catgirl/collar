package team.catgirl.collar.server.managers;

import com.google.common.collect.ImmutableList;
import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.messages.ClientMessage.*;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.messages.ServerMessage.*;
import team.catgirl.collar.models.Group;
import team.catgirl.collar.models.Group.Member;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.TokenGenerator;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class GroupManager {

    private static final Logger LOGGER = Logger.getLogger(GroupManager.class.getName());

    private final ServerIdentity serverIdentity;
    private final SessionManager sessionManager;

    private final ConcurrentMap<String, Group> groupsById = new ConcurrentHashMap<>();

    public GroupManager(ServerIdentity serverIdentity, SessionManager sessionManager) throws NoSuchAlgorithmException {
        this.serverIdentity = serverIdentity;
        this.sessionManager = sessionManager;
    }

    /**
     * Create a new group
     * @param req of the new group request
     * @return response to send to client
     */
    public CreateGroupResponse createGroup(PlayerIdentity identity, CreateGroupRequest req) {
        Group group = Group.newGroup(TokenGenerator.stringToken(), identity, req.position, req.players);
        synchronized (group.id) {
            refreshGroupState(group);
        }
        return new CreateGroupResponse(group);
    }

    /**
     * Accept a membership request
     * @param req of the new group request
     * @return response to send the client
     */
    public AcceptGroupMembershipResponse acceptMembership(PlayerIdentity identity, AcceptGroupMembershipRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            return new AcceptGroupMembershipResponse(null);
        }
        synchronized (group.id) {
            Group.MembershipState state = req.state;
            group = group.updateMemberState(identity.player, state);
            refreshGroupState(group);
        }
        return new AcceptGroupMembershipResponse(group);
    }

    /**
     * Leave the group
     * @param req to leave the group
     * @return response to client
     */
    public LeaveGroupResponse leaveGroup(PlayerIdentity identity, LeaveGroupRequest req) {
        Group group = groupsById.get(req.groupId);
        if (group == null) {
            LOGGER.log(Level.INFO, identity + " was not a member of the group " + req.groupId);
            return new LeaveGroupResponse(null);
        }
        synchronized (group.id) {
            group = group.removeMember(identity.player);
            refreshGroupState(group);
            sendMessageToGroup(identity.player, group, new UpdatePlayerStateResponse(ImmutableList.of(group)).serverMessage(serverIdentity));
        }
        LOGGER.log(Level.INFO, "Group count " + groupsById.size());
        return new LeaveGroupResponse(group.id);
    }

    /**
     * Removes player from all groups
     * @param player to remove
     */
    public void removeUserFromAllGroups(UUID player) {
        List<Group> groups = findGroupsForPlayer(player);
        groups.forEach(group -> {
            synchronized (group.id) {
                group = group.updateMemberState(player, Group.MembershipState.DECLINED);
                refreshGroupState(group);
            }
        });
        LOGGER.log(Level.INFO, "Removed user " + player + " from all groups");
    }

    /**
     * Sends membership requests to the group members
     * @param requester whos sending the request
     * @param group the group to invite to
     * @param members members to send requests to. If null, defaults to the full member list.
     */
    public void sendMembershipRequests(PlayerIdentity requester, Group group, List<Member> members) {
        synchronized (group.id) {
            Collection<Member> memberList = members == null ? group.members.values() : members;
            memberList.stream().filter(member -> member.membershipState == Group.MembershipState.PENDING).map(member -> member.player).forEach(player -> {
                Session session = sessionManager.getSession(player);
                if (session != null) {
                    try {
                        sessionManager.send(session, new GroupMembershipRequest(group.id, requester.player, ImmutableList.copyOf(group.members.keySet())).serverMessage(serverIdentity));
                    } catch (IOException e) {
                        LOGGER.log(Level.INFO, "Problem sending membership requests", e);
                        sessionManager.stopSession(session, "Could not communicate with player " + player, e);
                    }
                }
            });
        }
    }

    /**
     * Invite user to a group
     * @param groupInviteRequest
     */
    public GroupInviteResponse invite(PlayerIdentity identity, GroupInviteRequest groupInviteRequest) {
        Group group = groupsById.get(groupInviteRequest.groupId);
        if (group == null) {
            return new GroupInviteResponse(null, null);
        }
        List<Member> newMembers = new ArrayList<>();
        synchronized (group.id) {
            Member requester = group.members.get(identity.player);
            if (requester == null) {
                LOGGER.log(Level.INFO, identity.player + " is not a member of the group "  + group.id);
                return new GroupInviteResponse(group.id, null);
            }
            if (requester.membershipRole != Group.MembershipRole.OWNER) {
                LOGGER.log(Level.INFO, identity.player + " is not OWNER member of the group "  + group.id);
                return new GroupInviteResponse(group.id, null);
            }
            group = group.addMembers(groupInviteRequest.players, Group.MembershipRole.MEMBER, Group.MembershipState.PENDING, (newGroup, members) -> {
                sendMembershipRequests(identity, newGroup, members);
                newMembers.addAll(members);
            });
            refreshGroupState(group);
        }
        return new GroupInviteResponse(group.id, newMembers.stream().map(member -> member.player).collect(Collectors.toList()));
    }

    /**
     * Update the player state
     * @param req to update player position
     * @return response to send to client
     */
    public UpdatePlayerStateResponse updatePosition(PlayerIdentity identity, UpdatePlayerStateRequest req) {
        List<Group> groups = findGroupsForPlayer(identity.player);
        groups.forEach(group -> {
            synchronized (group.id) {
                group = group.updateMemberPosition(identity.player, req.position);
                refreshGroupState(group);
                sendMessageToGroup(identity.player, group, new UpdatePlayerStateResponse(groups).serverMessage(serverIdentity));
            }
        });
        return new UpdatePlayerStateResponse(findGroupsForPlayer(identity.player));
    }

    private void updateGroup(String groupId) {
        Group group = groupsById.get(groupId);
        if (group != null) {
            synchronized (group.id) {
                group.members.values().forEach(member -> {
                    List<Group> groupsForPlayer = findGroupsForPlayer(member.player);
                    if (!groupsForPlayer.isEmpty()) {
                        sendMessageToGroup(null, group, new UpdatePlayerStateResponse(groupsForPlayer).serverMessage(serverIdentity));
                    }
                });
            }
        }
    }

    private List<Group> findGroupsForPlayer(UUID player) {
        return groupsById.values().stream().filter(group -> group.members.containsKey(player)).collect(Collectors.toList());
    }

    private void sendMessageToGroup(UUID currentPlayer, Group group, ServerMessage message) {
        synchronized (group.id) {
            group.members.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(currentPlayer))
                    .filter(entry -> entry.getValue().membershipState == Group.MembershipState.ACCEPTED)
                    .forEach(entry -> {
                        Session session = sessionManager.getSession(entry.getKey());
                        if (session != null) {
                            try {
                                sessionManager.send(session, message);
                            } catch (IOException e) {
                                sessionManager.stopSession(session, "Could not communicate with player " + entry.getKey(), e);
                            }
                        }
                    });
        }
    }

    private void refreshGroupState(Group group) {
        synchronized (group.id) {
            if (group.members.isEmpty()) {
                LOGGER.log(Level.INFO, "Removed group " + group.id + " as it has no members.");
                groupsById.remove(group.id);
            } else {
                groupsById.put(group.id, group);
                updateGroup(group.id);
            }
        }
    }
}
