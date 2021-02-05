package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.List;
import java.util.UUID;

public final class GroupInviteRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("players")
    public final List<UUID> players;

    @JsonCreator
    public GroupInviteRequest(
            @JsonProperty("identify") ClientIdentity identity,
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("players") List<UUID> players) {
        super(identity);
        this.groupId = groupId;
        this.players = players;
    }
}
