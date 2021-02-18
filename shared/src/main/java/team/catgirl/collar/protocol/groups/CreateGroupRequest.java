package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("players")
    public final List<UUID> players;
    @JsonProperty("keys")
    public final byte[] keys;

    @JsonCreator
    public CreateGroupRequest(@JsonProperty("identity") ClientIdentity identity,
                              @JsonProperty("groupId") UUID groupId,
                              @JsonProperty("players") List<UUID> players,
                              @JsonProperty("keys") byte[] keys) {
        super(identity);
        this.groupId = groupId;
        this.players = players;
        this.keys = keys;
    }
}
