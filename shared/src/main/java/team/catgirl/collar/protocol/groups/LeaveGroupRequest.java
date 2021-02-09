package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

/**
 * Sent by the client to leave a group on their own accord
 */
public final class LeaveGroupRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;

    @JsonCreator
    public LeaveGroupRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("groupId") UUID groupId) {
        super(identity);
        this.groupId = groupId;
    }
}
