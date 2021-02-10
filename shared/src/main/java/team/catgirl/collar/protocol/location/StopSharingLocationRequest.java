package team.catgirl.collar.protocol.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

/**
 * Sent by a client when it wants to stop sharing its location
 */
public final class StopSharingLocationRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;

    public StopSharingLocationRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("groupId") UUID groupId) {
        super(identity);
        this.groupId = groupId;
    }
}
