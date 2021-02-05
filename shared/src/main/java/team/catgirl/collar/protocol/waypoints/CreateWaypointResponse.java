package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public abstract class CreateWaypointResponse extends ProtocolResponse {
    public final UUID groupId;

    @JsonCreator
    public CreateWaypointResponse(@JsonProperty("identity") ServerIdentity identity, UUID groupId) {
        super(identity);
        this.groupId = groupId;
    }

    public static final class CreateWaypointSuccessResponse extends CreateWaypointResponse {
        @JsonProperty("waypoint")
        public final Waypoint waypoint;

        @JsonCreator
        public CreateWaypointSuccessResponse(
                @JsonProperty("identity") ServerIdentity identity,
                @JsonProperty("groupId") UUID groupId,
                @JsonProperty("waypoint") Waypoint waypoint) {
            super(identity, groupId);
            this.waypoint = waypoint;
        }
    }

    public final static class CreateWaypointFailedResponse extends CreateWaypointResponse {
        @JsonProperty("name")
        public final String waypointName;

        @JsonCreator
        public CreateWaypointFailedResponse(
                @JsonProperty("identity") ServerIdentity identity,
                @JsonProperty("groupId") UUID groupId,
                @JsonProperty("name") String waypointName) {
            super(identity, groupId);
            this.waypointName = waypointName;
        }
    }
}
