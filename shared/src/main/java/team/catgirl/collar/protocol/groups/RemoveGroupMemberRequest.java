package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class RemoveGroupMemberRequest extends ProtocolRequest {

    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final UUID player;

    public RemoveGroupMemberRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("groupId") UUID groupId, @JsonProperty("player") UUID player) {
        super(identity);
        this.groupId = groupId;
        this.player = player;
    }
}
