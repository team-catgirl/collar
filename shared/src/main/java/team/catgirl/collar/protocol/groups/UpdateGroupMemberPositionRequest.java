package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Position;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class UpdateGroupMemberPositionRequest extends ProtocolRequest {
    @JsonProperty("position")
    public final Position position;

    public UpdateGroupMemberPositionRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("position") Position position) {
        super(identity);
        this.position = position;
    }
}
