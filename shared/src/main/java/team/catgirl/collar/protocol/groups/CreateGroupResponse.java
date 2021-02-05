package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public final class CreateGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final Group group;

    @JsonCreator
    public CreateGroupResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("group") Group group) {
        super(identity);
        this.group = group;
    }
}
