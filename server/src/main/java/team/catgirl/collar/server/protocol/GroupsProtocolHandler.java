package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.services.groups.GroupService;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GroupsProtocolHandler extends ProtocolHandler {
    private static final Logger LOGGER = Logger.getLogger(GroupsProtocolHandler.class.getName());

    private final GroupService groups;

    public GroupsProtocolHandler(GroupService groups) {
        this.groups = groups;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        ProtocolResponse resp;
        if (req instanceof CreateGroupRequest) {
            LOGGER.log(Level.INFO, "CreateGroupRequest received from " + req.identity);
            CreateGroupRequest request = (CreateGroupRequest)req;
            resp = groups.createGroup(request);
        } else if (req instanceof JoinGroupRequest) {
            LOGGER.log(Level.INFO, "AcceptGroupMembershipRequest received from " + req.identity);
            JoinGroupRequest request = (JoinGroupRequest) req;
            resp = groups.acceptMembership(request);
        } else if (req instanceof LeaveGroupRequest) {
            LOGGER.log(Level.INFO, "LeaveGroupRequest received from " + req.identity);
            LeaveGroupRequest request = (LeaveGroupRequest)req;
            resp = groups.leaveGroup(request);
        } else if (req instanceof GroupInviteRequest) {
            LOGGER.log(Level.INFO, "GroupInviteRequest received from " + req.identity);
            GroupInviteRequest request = (GroupInviteRequest)req;
            resp = groups.invite(request);
        } else if (req instanceof EjectGroupMemberRequest) {
            EjectGroupMemberRequest request = (EjectGroupMemberRequest) req;
            resp = groups.ejectMember(request);
        } else if (req instanceof AcknowledgedGroupJoinedRequest) {
            AcknowledgedGroupJoinedRequest request = (AcknowledgedGroupJoinedRequest) req;
            resp = groups.acknowledgeJoin(request);
        } else {
            resp = null;
        }
        if (resp != null) {
            sender.accept(req.identity, resp);
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, MinecraftPlayer player, BiConsumer<Session, ProtocolResponse> sender) {
        if (player != null) {
            sender.accept(null, groups.removeUserFromAllGroups(player));
        }
    }
}
