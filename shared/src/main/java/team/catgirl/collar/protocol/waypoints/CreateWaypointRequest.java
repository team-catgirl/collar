package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class CreateWaypointRequest extends ProtocolRequest {
    @JsonProperty("waypointId")
    public final UUID waypointId;
    @JsonProperty("waypoint")
    public final byte[] waypoint;

    public CreateWaypointRequest(@JsonProperty("identity") ClientIdentity identity,
                                 @JsonProperty("waypointId") UUID waypointId,
                                 @JsonProperty("waypoint") byte[] waypoint) {
        super(identity);
        this.waypointId = waypointId;
        this.waypoint = waypoint;
    }
}
