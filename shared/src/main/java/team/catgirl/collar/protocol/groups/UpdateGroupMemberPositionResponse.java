package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.List;

public final class UpdateGroupMemberPositionResponse extends ProtocolResponse {
    @JsonProperty("groups")
    public final List<Group> groups;

    public UpdateGroupMemberPositionResponse(@JsonProperty("group") ServerIdentity identity, @JsonProperty("groups") List<Group> groups) {
        super(identity);
        this.groups = groups;
    }
}
