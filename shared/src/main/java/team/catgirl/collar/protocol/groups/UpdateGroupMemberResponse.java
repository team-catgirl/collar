package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.friends.Status;
import team.catgirl.collar.api.groups.MembershipRole;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public final class UpdateGroupMemberResponse extends ProtocolResponse {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("status")
    public final Status status;
    @JsonProperty("role")
    public final MembershipRole role;

    public UpdateGroupMemberResponse(@JsonProperty("identity") ServerIdentity serverIdentity,
                                     @JsonProperty("groupId") UUID groupId,
                                     @JsonProperty("sender") Player player,
                                     @JsonProperty("status") Status status,
                                     @JsonProperty("role") MembershipRole role) {
        super(serverIdentity);
        this.groupId = groupId;
        this.player = player;
        this.status = status;
        this.role = role;
    }
}
