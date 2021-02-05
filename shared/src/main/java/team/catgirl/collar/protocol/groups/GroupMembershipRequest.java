package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.List;
import java.util.UUID;

public final class GroupMembershipRequest extends ProtocolResponse {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final MinecraftPlayer player;
    @JsonProperty("members")
    public final List<MinecraftPlayer> members;

    @JsonCreator
    public GroupMembershipRequest(
            @JsonProperty("identity") ServerIdentity identity,
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("player") MinecraftPlayer player,
            @JsonProperty("members") List<MinecraftPlayer> members) {
        super(identity);
        this.groupId = groupId;
        this.player = player;
        this.members = members;
    }
}
