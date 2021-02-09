package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.UUID;

/**
 * Sent to all group members whenever someone leaves the group
 */
public final class LeaveGroupResponse extends ProtocolResponse {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final MinecraftPlayer player;

    @JsonCreator
    public LeaveGroupResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("groupId") UUID groupId, @JsonProperty("player") MinecraftPlayer player) {
        super(identity);
        this.groupId = groupId;
        this.player = player;
    }
}
