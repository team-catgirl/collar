package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.Group.MembershipState;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class AcceptGroupMembershipRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("state")
    public final MembershipState state;

    public AcceptGroupMembershipRequest(
            @JsonProperty("identity") ClientIdentity identity,
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("state") MembershipState state) {
        super(identity);
        this.groupId = groupId;
        this.state = state;
    }
}
