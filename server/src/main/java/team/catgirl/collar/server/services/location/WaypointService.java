package team.catgirl.collar.server.services.location;

import team.catgirl.collar.api.groups.MembershipState;
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointResponse;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.session.SessionManager;

/**
 * Manages {@link team.catgirl.collar.api.groups.Group} and {@link MinecraftPlayer} waypoints
 */
public final class WaypointService {
    private final ServerIdentity serverIdentity;
    private final GroupService groups;
    private final SessionManager sessions;

    public WaypointService(ServerIdentity serverIdentity, GroupService groups, SessionManager sessions) {
        this.serverIdentity = serverIdentity;
        this.groups = groups;
        this.sessions = sessions;
    }

    public BatchProtocolResponse createWaypoint(CreateWaypointRequest req) {
        if (req.group == null) {
            throw new IllegalStateException("cannot create private waypoints yet");
        } else {
            MinecraftPlayer sendingPlayer = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("cannot find player for " + req.identity));
            return groups.findGroup(req.group).map(group -> {
                return groups.createMemberMessages(group,
                        member -> member.membershipState.equals(MembershipState.ACCEPTED) && !member.player.equals(sendingPlayer),
                        (identity, player, updatedMember) -> new CreateWaypointResponse(serverIdentity, group.id, req.identity, req.waypointId, req.waypoint));
            })
            .orElse(new BatchProtocolResponse(serverIdentity));
        }
    }

    public BatchProtocolResponse removeWaypoint(RemoveWaypointRequest req) {
        if (req.group == null) {
            throw new IllegalStateException("cannot remove private waypoints yet");
        } else {
            return groups.findGroup(req.group).map(group -> {
                return groups.createMemberMessages(group,
                        member -> member.membershipState.equals(MembershipState.ACCEPTED),
                        (identity, player, updatedMember) -> new RemoveWaypointResponse(serverIdentity, group.id, req.waypointId));
            }).orElse(new BatchProtocolResponse(serverIdentity));
        }
    }
}
