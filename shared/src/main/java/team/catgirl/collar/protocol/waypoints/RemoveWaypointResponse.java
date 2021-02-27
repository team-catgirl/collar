package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public final class RemoveWaypointResponse extends ProtocolResponse {

    @JsonProperty("waypointId")
    public final UUID waypointId;

    @JsonCreator
    public RemoveWaypointResponse(@JsonProperty("identity") ServerIdentity identity,
                                  @JsonProperty("waypointId") UUID waypointId) {
        super(identity);
        this.waypointId = waypointId;
    }
}
