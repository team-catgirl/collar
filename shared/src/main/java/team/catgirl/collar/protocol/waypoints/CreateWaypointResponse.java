package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public final class CreateWaypointResponse extends ProtocolResponse {
    @JsonProperty("sender")
    public final ClientIdentity sender;

    @JsonProperty("waypointId")
    public final UUID waypointId;

    @JsonProperty("waypoint")
    public final byte[] waypoint;

    @JsonCreator
    public CreateWaypointResponse(
            @JsonProperty("identity") ServerIdentity identity,
            @JsonProperty("sender") ClientIdentity sender,
            @JsonProperty("waypointId") UUID waypointId,
            @JsonProperty("waypoint") byte[] waypoint) {
        super(identity);
        this.sender = sender;
        this.waypointId = waypointId;
        this.waypoint = waypoint;
    }
}
