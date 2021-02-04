package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public final class RemoveGroupMemberResponse extends ProtocolResponse {

    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final UUID player;

    public RemoveGroupMemberResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("groupId") UUID groupId, @JsonProperty("player") UUID player) {
        super(identity);
        this.groupId = groupId;
        this.player = player;
    }
}
