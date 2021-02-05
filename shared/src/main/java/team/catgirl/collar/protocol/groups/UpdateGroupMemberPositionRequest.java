package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class UpdateGroupMemberPositionRequest extends ProtocolRequest {
    @JsonProperty("position")
    public final Location location;

    @JsonCreator
    public UpdateGroupMemberPositionRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("position") Location location) {
        super(identity);
        this.location = location;
    }
}
