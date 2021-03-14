package team.catgirl.collar.server.services.location;

import com.google.common.collect.Sets;
import team.catgirl.collar.api.groups.MemberSource;
import team.catgirl.collar.api.session.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * State machine for managing dynamically created {@link team.catgirl.collar.api.groups.Group}'s based on hashing
 * every players player entity list and comparing them.
 */
public final class NearbyGroups {

    private final ConcurrentMap<MemberSource, Set<String>> playerHashes = new ConcurrentHashMap<>();
    private final ConcurrentMap<NearbyGroup, UUID> nearbyGroups = new ConcurrentHashMap<>();
    private final ConcurrentMap<MemberSource, Set<NearbyGroup>> playerToGroups = new ConcurrentHashMap<>();

    /**
     * Calculates any nearby groups for the minecraft player and anyone in the calculated group
     * Both parties must have entity hashes in common to form one or more groups with other players
     * @param source to create state for
     * @param hashes the players hashes
     * @return result delta
     */
    public Result updateNearbyGroups(MemberSource source, Set<String> hashes) {
        playerHashes.compute(source, (thePlayer, strings) -> hashes);
        Map<UUID, NearbyGroup> add = new HashMap<>();
        Map<UUID, NearbyGroup> remove = new HashMap<>();
        playerHashes.keySet().stream()
                .filter(anotherPlayer -> anotherPlayer.player.minecraftPlayer.inServerWith(source.player.minecraftPlayer)
                        && !anotherPlayer.player.minecraftPlayer.equals(source.player.minecraftPlayer)
                ).forEach(anotherPlayer -> {
            Set<String> otherPlayersHashes = playerHashes.get(anotherPlayer);
            NearbyGroup group = new NearbyGroup(Set.of(source, anotherPlayer));
            if (Sets.difference(hashes, otherPlayersHashes).isEmpty()) {
                nearbyGroups.compute(group, (nearbyGroup, uuid) -> {
                    if (uuid == null) {
                        uuid = UUID.randomUUID();
                        add.put(uuid, group);
                    }
                    return uuid;
                });
                playerToGroups.compute(source, (thePlayer, playersGroups) -> {
                    playersGroups = playersGroups == null ? new HashSet<>() : playersGroups;
                    playersGroups.add(group);
                    return playersGroups;
                });
            } else {
                UUID groupId = nearbyGroups.get(group);
                if (groupId != null) {
                    remove.put(groupId, group);
                    playerToGroups.compute(source, (thePlayer, playersGroups) -> {
                        playersGroups = playersGroups == null ? new HashSet<>() : playersGroups;
                        playersGroups.remove(group);
                        return nearbyGroups.isEmpty() ? null : playersGroups;
                    });
                }
            }
        });
        return new Result(add, remove);
    }

    /**
     * Returns the groups to leave
     * @param player to remove
     */
    public void removePlayerState(Player player) {
        playerHashes.remove(player);
        Set<NearbyGroup> groups = playerToGroups.remove(player);
        if (groups != null) {
            groups.forEach(nearbyGroups::remove);
        }
    }

    public static final class Result {
        public final Map<UUID, NearbyGroup> add;
        public final Map<UUID, NearbyGroup> remove;

        public Result(Map<UUID, NearbyGroup> add, Map<UUID, NearbyGroup> remove) {
            this.add = add;
            this.remove = remove;
        }
    }
}
