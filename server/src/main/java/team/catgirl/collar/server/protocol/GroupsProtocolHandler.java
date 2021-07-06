package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.services.groups.GroupService;

import java.util.Optional;
import java.util.function.BiConsumer;
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
        Optional<? extends ProtocolResponse> resp;
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
        } else if (req instanceof DeleteGroupRequest) {
            DeleteGroupRequest request = (DeleteGroupRequest) req;
            resp = groups.delete(request);
        } else if (req instanceof TransferGroupOwnershipRequest) {
            TransferGroupOwnershipRequest request = (TransferGroupOwnershipRequest) req;
            resp = groups.transferOwnership(request);
        } else {
            resp = Optional.empty();
        }
        resp.ifPresent(protocolResponse -> {
            sender.accept(req.identity, protocolResponse);
        });
        return resp.isPresent();
    }

    @Override
    public void onSessionStarted(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {
        super.onSessionStarted(identity, player, sender);
        groups.playerIsOnline(identity, player).ifPresent(response -> sender.accept(null, response));
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {
        super.onSessionStopping(identity, player, sender);
        if (player == null) return;
        groups.playerIsOffline(player).ifPresent(response -> {
            sender.accept(null, response);
        });
    }
}
