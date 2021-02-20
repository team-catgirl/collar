package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.Group.MembershipState;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

/**
 * Accept an invitation to the group
 */
public final class JoinGroupRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("state")
    public final MembershipState state;
    @JsonProperty("keys")
    public final byte[] keys;

    @JsonCreator
    public JoinGroupRequest(
            @JsonProperty("identity") ClientIdentity identity,
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("state") MembershipState state,
            @JsonProperty("keys") byte[] keys) {
        super(identity);
        this.groupId = groupId;
        this.state = state;
        this.keys = keys;
    }
}
