package team.catgirl.collar.protocol.waypoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public abstract class RemoveWaypointResponse extends ProtocolResponse {

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

    public static final class RemoveWaypointSuccessResponse extends RemoveWaypointResponse {
        @JsonCreator
        public RemoveWaypointSuccessResponse(@JsonProperty("identity") ServerIdentity identity,
                                             @JsonProperty("groupId") UUID groupId,
                                             @JsonProperty("waypointId") UUID waypointId) {
            super(identity, groupId, waypointId);
        }
    }

    public static final class RemoveWaypointFailedResponse extends RemoveWaypointResponse {
        @JsonCreator
        public RemoveWaypointFailedResponse(@JsonProperty("identity")ServerIdentity identity,
                                            @JsonProperty("groupId") UUID groupId,
                                            @JsonProperty("waypointId") UUID waypointId) {
            super(identity, groupId, waypointId);
        }
    }
}
