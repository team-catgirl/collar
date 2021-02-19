package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

/**
 * Sent on response to {@link JoinGroupResponse} to distribute all keys
 */
public final class AcknowledgedGroupJoinedRequest extends ProtocolRequest {
    @JsonProperty("recipient")
    public final ClientIdentity recipient;

    @JsonProperty("group")
    public final UUID group;

    @JsonProperty("keys")
    public final byte[] keys;

    public AcknowledgedGroupJoinedRequest(@JsonProperty("identity") ClientIdentity identity,
                                          @JsonProperty("recipient") ClientIdentity recipient,
                                          @JsonProperty("group") UUID group,
                                          @JsonProperty("keys") byte[] keys) {
        super(identity);
        this.recipient = recipient;
        this.group = group;
        this.keys = keys;
    }
}
