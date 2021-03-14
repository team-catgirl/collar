package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.session.Player;

import java.util.Objects;

public class Member {
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("profile")
    public final PublicProfile profile;
    @JsonProperty("role")
    public final MembershipRole membershipRole;
    @JsonProperty("state")
    public final MembershipState membershipState;

    public Member(
            @JsonProperty("player") Player player,
            @JsonProperty("profile") PublicProfile profile,
            @JsonProperty("role") MembershipRole membershipRole,
            @JsonProperty("state") MembershipState membershipState) {
        this.player = player;
        this.profile = profile;
        this.membershipRole = membershipRole;
        this.membershipState = membershipState;
    }

    public Member updateMembershipState(MembershipState membershipState) {
        return new Member(player, profile, membershipRole, membershipState);
    }

    public Member updateMembershipRole(MembershipRole newMembershipRole) {
        return new Member(player, profile, newMembershipRole, membershipState);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return player.equals(member.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player);
    }
}
