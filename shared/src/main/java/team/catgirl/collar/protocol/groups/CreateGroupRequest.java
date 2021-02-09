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
    @JsonProperty("players")
    public final List<UUID> players;

    @JsonCreator
    public CreateGroupRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("players") List<UUID> players) {
        super(identity);
        this.players = players;
    }
}
