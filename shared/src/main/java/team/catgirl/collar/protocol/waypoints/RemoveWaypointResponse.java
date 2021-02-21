package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public class RemoveWaypointResponse extends ProtocolResponse {

    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("waypointId")
    public final UUID waypointId;

    @JsonCreator
    public RemoveWaypointResponse(@JsonProperty("identity") ServerIdentity identity,
                                  @JsonProperty("groupId") UUID groupId,
                                  @JsonProperty("waypointId") UUID waypointId) {
        super(identity);
        this.groupId = groupId;
        this.waypointId = waypointId;
    }
}
