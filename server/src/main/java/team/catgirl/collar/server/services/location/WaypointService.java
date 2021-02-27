package team.catgirl.collar.server.services.location;

import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.session.SessionManager;

/**
 * Manages private waypoints
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
        throw new IllegalStateException("cannot create private waypoints yet");
    }

    public BatchProtocolResponse removeWaypoint(RemoveWaypointRequest req) {
        throw new IllegalStateException("cannot remove private waypoints yet");
    }
}
