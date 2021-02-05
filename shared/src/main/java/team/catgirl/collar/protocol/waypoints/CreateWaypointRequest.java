package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Position;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class CreateWaypointRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("name")
    public final String waypointName;
    @JsonProperty("position")
    public final Position position;

    @JsonCreator
    public CreateWaypointRequest(
            @JsonProperty("identity") ClientIdentity identity,
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("name") String waypointName,
            @JsonProperty("position") Position position) {
        super(identity);
        this.groupId = groupId;
        this.waypointName = waypointName;
        this.position = position;
    }
}
