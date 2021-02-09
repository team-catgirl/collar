package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public final class JoinGroupResponse extends ProtocolResponse {
    @JsonProperty("group")
    public final Group group;
    @JsonProperty("player")
    public final MinecraftPlayer player;

    @JsonCreator
    public JoinGroupResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("group") Group group, @JsonProperty("player") MinecraftPlayer player) {
        super(identity);
        this.group = group;
        this.player = player;
    }
}
