package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.GroupType;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

/**
 * Sent by the group owner to invite players to an existing group using a {@link GroupInviteRequest}
 */
public final class GroupInviteResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("type")
    public final GroupType type;
    @JsonProperty("sender")
    public final Player sender;

    @JsonCreator
    public GroupInviteResponse(
            @JsonProperty("identity") ServerIdentity identity,
            @JsonProperty("group") UUID group,
            @JsonProperty("name") String name,
            @JsonProperty("type") GroupType type,
            @JsonProperty("sender") Player sender) {
        super(identity);
        this.group = group;
        this.name = name;
        this.type = type;
        this.sender = sender;
    }
}
