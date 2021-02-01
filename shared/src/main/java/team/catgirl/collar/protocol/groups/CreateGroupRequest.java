package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.List;
import java.util.UUID;

public final class CreateGroupRequest extends ProtocolRequest {
    @JsonProperty("players")
    public final List<UUID> players;

    public CreateGroupRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("players") List<UUID> players) {
        super(identity);
        this.players = players;
    }
}
