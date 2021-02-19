package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public final class AcknowledgedGroupJoinedResponse extends ProtocolResponse {
    @JsonProperty("sender")
    public final ClientIdentity sender;

    @JsonProperty("group")
    public final UUID group;

    @JsonProperty("keys")
    public final byte[] keys;

    public AcknowledgedGroupJoinedResponse(@JsonProperty("identity") ServerIdentity identity,
                                           @JsonProperty("sender") ClientIdentity sender,
                                           @JsonProperty("group") UUID group,
                                           @JsonProperty("keys") byte[] keys) {
        super(identity);
        this.sender = sender;
        this.group = group;
        this.keys = keys;
    }
}
