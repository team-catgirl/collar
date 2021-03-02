package team.catgirl.collar.client.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.groups.GroupInviteResponse;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.List;
import java.util.UUID;

public final class GroupInvitation {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("members")
    public final List<Player> members;

    public GroupInvitation(UUID groupId, Player player, List<Player> members) {
        this.groupId = groupId;
        this.player = player;
        this.members = members;
    }

    public static GroupInvitation from(GroupInviteResponse req) {
        return new GroupInvitation(req.groupId, req.player, req.members);
    }
}
