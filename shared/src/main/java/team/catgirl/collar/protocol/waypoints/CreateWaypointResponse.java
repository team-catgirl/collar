package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public class CreateWaypointResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final UUID group;

    @JsonProperty("sender")
    public final ClientIdentity sender;

    @JsonProperty("waypointId")
    public final UUID waypointId;

    @JsonProperty("waypoint")
    public final byte[] waypoint;

    @JsonCreator
    public CreateWaypointResponse(
            @JsonProperty("identity") ServerIdentity identity,
            @JsonProperty("group") UUID group,
            @JsonProperty("sender") ClientIdentity sender,
            @JsonProperty("waypointId") UUID waypointId,
            @JsonProperty("waypoint") byte[] waypoint) {
        super(identity);
        this.group = group;
        this.sender = sender;
        this.waypointId = waypointId;
        this.waypoint = waypoint;
    }
}
