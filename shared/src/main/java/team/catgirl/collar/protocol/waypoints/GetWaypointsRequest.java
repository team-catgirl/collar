package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class GetWaypointsRequest extends ProtocolRequest {
    public GetWaypointsRequest(@JsonProperty("identity") ClientIdentity identity) {
        super(identity);
    }
}
