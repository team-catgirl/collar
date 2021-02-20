package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public final class JoinGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final Group group;
    @JsonProperty("sender")
    public final ClientIdentity sender;
    @JsonProperty("player")
    public final MinecraftPlayer player;
    @JsonProperty("keys")
    public final byte[] keys;

    @JsonCreator
    public JoinGroupResponse(@JsonProperty("identity") ServerIdentity identity,
                             @JsonProperty("group") Group group,
                             @JsonProperty("sender") ClientIdentity sender,
                             @JsonProperty("player") MinecraftPlayer player,
                             @JsonProperty("keys") byte[] keys) {
        super(identity);
        this.group = group;
        this.sender = sender;
        this.player = player;
        this.keys = keys;
    }
}
