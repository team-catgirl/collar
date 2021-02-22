package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.UUID;

public final class AcknowledgedGroupJoinedResponse extends ProtocolResponse {
    @JsonProperty("sender")
    public final ClientIdentity sender;

    @JsonProperty("player")
    public final MinecraftPlayer player;

    @JsonProperty("group")
    public final Group group;

    @JsonProperty("keys")
    public final byte[] keys;

    public AcknowledgedGroupJoinedResponse(@JsonProperty("identity") ServerIdentity identity,
                                           @JsonProperty("sender") ClientIdentity sender,
                                           @JsonProperty("player") MinecraftPlayer player,
                                           @JsonProperty("group") Group group,
                                           @JsonProperty("keys") byte[] keys) {
        super(identity);
        this.sender = sender;
        this.player = player;
        this.group = group;
        this.keys = keys;
    }
}
