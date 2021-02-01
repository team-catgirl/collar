package team.catgirl.collar.client.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.groups.GroupMembershipRequest;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.List;
import java.util.UUID;

public final class GroupInvitation {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final MinecraftPlayer player;
    @JsonProperty("members")
    public final List<MinecraftPlayer> members;

    public GroupInvitation(UUID groupId, MinecraftPlayer player, List<MinecraftPlayer> members) {
        this.groupId = groupId;
        this.player = player;
        this.members = members;
    }

    public static GroupInvitation from(GroupMembershipRequest req) {
        return new GroupInvitation(req.groupId, req.player, req.members);
    }
}
