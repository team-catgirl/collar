package team.catgirl.collar.api.groups;

import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.session.Player;

import java.util.Objects;

public final class MemberSource {
    public final Player player;
    public final PublicProfile profile;

    public MemberSource(Player player, PublicProfile profile) {
        this.player = player;
        this.profile = profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberSource source = (MemberSource) o;
        return player.equals(source.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player);
    }
}
