package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class RemoveWaypointRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("waypointId")
    public final UUID waypointId;

    @JsonCreator
    public RemoveWaypointRequest(
            @JsonProperty("identity") ClientIdentity identity,
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("waypointId") UUID waypointId) {
        super(identity);
        this.groupId = groupId;
        this.waypointId = waypointId;
    }
}
