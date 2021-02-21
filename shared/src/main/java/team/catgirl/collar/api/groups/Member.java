package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public class Member {
    @JsonProperty("player")
    public final MinecraftPlayer player;
    @JsonProperty("role")
    public final MembershipRole membershipRole;
    @JsonProperty("state")
    public final MembershipState membershipState;
    @JsonProperty("position")
    public final Location location;

    public Member(
            @JsonProperty("player") MinecraftPlayer player,
            @JsonProperty("role") MembershipRole membershipRole,
            @JsonProperty("state") MembershipState membershipState,
            @JsonProperty("position") Location location) {
        this.player = player;
        this.membershipRole = membershipRole;
        this.membershipState = membershipState;
        this.location = location;
    }

    public Member updateMembershipState(MembershipState membershipState) {
        return new Member(player, membershipRole, membershipState, location);
    }

    public Member updatePosition(Location location) {
        return new Member(player, membershipRole, membershipState, location);
    }
}
