package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.waypoints.EncryptedWaypoint;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.List;

public final class GetWaypointsResponse extends ProtocolResponse {
    @JsonProperty("waypoints")
    public final List<EncryptedWaypoint> waypoints;

    public GetWaypointsResponse(
            @JsonProperty("identity") ServerIdentity identity,
            @JsonProperty("waypoints") List<EncryptedWaypoint> waypoints) {
        super(identity);
        this.waypoints = waypoints;
    }
}
