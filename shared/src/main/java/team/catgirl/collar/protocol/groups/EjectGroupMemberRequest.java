package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

/**
 * Sent by the group owner to forcefully remove a player from the group
 * Sender and all members recieve a {@link LeaveGroupResponse}
 */
public final class EjectGroupMemberRequest extends ProtocolRequest {

    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final UUID player;

    @JsonCreator
    public EjectGroupMemberRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("groupId") UUID groupId, @JsonProperty("player") UUID player) {
        super(identity);
        this.groupId = groupId;
        this.player = player;
    }
}
