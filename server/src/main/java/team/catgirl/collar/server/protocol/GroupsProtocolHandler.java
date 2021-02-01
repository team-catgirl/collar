package team.catgirl.collar.server.protocol;

import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.services.groups.GroupService;

import java.util.function.Consumer;

public final class GroupsProtocolHandler extends ProtocolHandler {
    private final GroupService groups;

    public GroupsProtocolHandler(GroupService groups) {
        this.groups = groups;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, Consumer<ProtocolResponse> sender) {
        ProtocolResponse resp;
        if (req instanceof CreateGroupRequest) {
            CreateGroupRequest request = (CreateGroupRequest)req;
            resp = groups.createGroup(request);
        } else if (req instanceof AcceptGroupMembershipRequest) {
            AcceptGroupMembershipRequest request = (AcceptGroupMembershipRequest) req;
            resp = groups.acceptMembership(request);
        } else if (req instanceof LeaveGroupRequest) {
            LeaveGroupRequest request = (LeaveGroupRequest)req;
            resp = groups.leaveGroup(request);
        } else if (req instanceof GroupInviteRequest) {
            GroupInviteRequest request = (GroupInviteRequest)req;
            resp = groups.invite(request);
        } else if (req instanceof UpdateGroupMemberPositionRequest) {
            UpdateGroupMemberPositionRequest request = (UpdateGroupMemberPositionRequest)req;
            resp = groups.updatePosition(request);
        } else {
            resp = null;
        }
        if (resp != null) {
            sender.accept(resp);
            return true;
        }
        return false;
    }
}
