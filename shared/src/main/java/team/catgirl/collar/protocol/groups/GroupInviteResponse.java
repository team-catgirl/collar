package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.List;
import java.util.UUID;

public final class GroupInviteResponse extends ProtocolResponse {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("players")
    public final List<MinecraftPlayer> players;

    public GroupInviteResponse(
            @JsonProperty("identity") ServerIdentity identity,
            @JsonProperty("groupId") UUID groupId,
            @JsonProperty("players") List<MinecraftPlayer> players) {
        super(identity);
        this.groupId = groupId;
        this.players = players;
    }
}
