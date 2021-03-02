package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class DeleteGroupRequest extends ProtocolRequest {
    @JsonProperty("group")
    public final UUID group;

    public DeleteGroupRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("group") UUID group) {
        super(identity);
        this.group = group;
    }
}
