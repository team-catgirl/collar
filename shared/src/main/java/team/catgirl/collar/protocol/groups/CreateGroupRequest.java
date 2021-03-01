package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.GroupType;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.List;
import java.util.UUID;

/**
 * Create a new group and send {@link GroupInviteRequest}'s for all players in `players`
 */
public final class CreateGroupRequest extends ProtocolRequest {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("type")
    public final GroupType type;
    @JsonProperty("players")
    public final List<UUID> players;

    @JsonCreator
    public CreateGroupRequest(@JsonProperty("identity") ClientIdentity identity,
                              @JsonProperty("groupId") UUID groupId,
                              @JsonProperty("name") String name,
                              @JsonProperty("type") GroupType type,
                              @JsonProperty("players") List<UUID> players) {
        super(identity);
        this.groupId = groupId;
        this.name = name;
        this.type = type;
        this.players = players;
    }
}
