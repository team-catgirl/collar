package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

/**
 * Sent to the group member when they reconnect
 */
public final class RejoinGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final UUID group;

    public RejoinGroupResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("group") UUID group) {
        super(identity);
        this.group = group;
    }
}
