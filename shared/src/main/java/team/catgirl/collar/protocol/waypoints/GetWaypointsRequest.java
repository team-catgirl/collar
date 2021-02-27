package team.catgirl.collar.protocol.waypoints;

import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class GetWaypointsRequest extends ProtocolRequest {
    public GetWaypointsRequest(ClientIdentity identity) {
        super(identity);
    }
}
