package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

/**
 * Sent back to you when your keys are distributed and the Group is ready to use
 */
public final class AcknowledgedGroupJoinedResponse extends ProtocolResponse {
    @JsonProperty("sender")
    public final ClientIdentity sender;

    @JsonProperty("player")
    public final Player player;

    @JsonProperty("group")
    public final Group group;

    @JsonProperty("keys")
    public final byte[] keys;

    public AcknowledgedGroupJoinedResponse(@JsonProperty("identity") ServerIdentity identity,
                                           @JsonProperty("sender") ClientIdentity sender,
                                           @JsonProperty("player") Player player,
                                           @JsonProperty("group") Group group,
                                           @JsonProperty("keys") byte[] keys) {
        super(identity);
        this.sender = sender;
        this.player = player;
        this.group = group;
        this.keys = keys;
    }
}
