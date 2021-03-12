package team.catgirl.collar.api.friends;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.profiles.PublicProfile;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a Friend
 */
public final class Friend {
    /** The profile that owns this record */
    @JsonProperty("owner")
    public final UUID owner;

    /**
     * Who the owner is friends with
     */
    @JsonProperty("friend")
    public final PublicProfile friend;

    /**
     * The current status of the friend
     */
    @JsonProperty("status")
    public final Status status;

    /**
     * The Minecraft player id's of this friend
     * Note: not using MinecraftPlayer here as it would leak server ip info to friends
     */
    @JsonProperty("playerIds")
    public final Set<UUID> playerIds;

    @JsonCreator
    public Friend(@JsonProperty("owner") UUID owner,
                  @JsonProperty("friendsWith") PublicProfile friend,
                  @JsonProperty("status") Status status,
                  @JsonProperty("playerIds") Set<UUID> playerIds) {
        this.owner = owner;
        this.friend = friend;
        this.status = status;
        this.playerIds = playerIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend1 = (Friend) o;
        return owner.equals(friend1.owner) && friend.equals(friend1.friend);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, friend);
    }
}
