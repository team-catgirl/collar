package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public final class LeaveGroupResponse extends ProtocolResponse {
    @JsonProperty("groupId")
    public final UUID groupId;

    @JsonCreator
    public LeaveGroupResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("groupId") UUID groupId) {
        super(identity);
        this.groupId = groupId;
    }
}
