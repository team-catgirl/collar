package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.UUID;

public class Member {
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("role")
    public final MembershipRole membershipRole;
    @JsonProperty("state")
    public final MembershipState membershipState;

    public Member(
            @JsonProperty("player") Player player,
            @JsonProperty("role") MembershipRole membershipRole,
            @JsonProperty("state") MembershipState membershipState) {
        this.player = player;
        this.membershipRole = membershipRole;
        this.membershipState = membershipState;
    }

    public Member updateMembershipState(MembershipState membershipState) {
        return new Member(player, membershipRole, membershipState);
    }

    public Member updatePosition(Location location) {
        return new Member(player, membershipRole, membershipState);
    }
}
