package team.catgirl.collar.server.services.location;

import com.google.common.collect.Sets;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * State machine for managing dynamically created {@link team.catgirl.collar.api.groups.Group}'s based on hashing
 * every players player entity list and comparing them.
 */
public final class NearbyGroups {

    private final ConcurrentMap<MinecraftPlayer, Set<String>> playerHashes = new ConcurrentHashMap<>();
    private final ConcurrentMap<NearbyGroup, UUID> nearbyGroups = new ConcurrentHashMap<>();
    private final ConcurrentMap<MinecraftPlayer, Set<NearbyGroup>> playerToGroups = new ConcurrentHashMap<>();

    /**
     * Calculates any nearby groups for the minecraft player and anyone in the calculated group
     * Both parties must have entity hashes in common to form one or more groups with other players
     * @param player to create state for
     * @param hashes the players hashes
     * @return result delta
     */
    public Result updateNearbyGroups(MinecraftPlayer player, Set<String> hashes) {
        playerHashes.compute(player, (thePlayer, strings) -> hashes);
        Map<UUID, NearbyGroup> add = new HashMap<>();
        Map<UUID, NearbyGroup> remove = new HashMap<>();
        playerHashes.keySet().stream().filter(anotherPlayer -> anotherPlayer.inServerWith(player) && !anotherPlayer.equals(player)).forEach(anotherPlayer -> {
            Set<String> otherPlayersHashes = playerHashes.get(anotherPlayer);
            NearbyGroup group = new NearbyGroup(Set.of(player, anotherPlayer));
            if (Sets.difference(hashes, otherPlayersHashes).isEmpty()) {
                nearbyGroups.compute(group, (nearbyGroup, uuid) -> {
                    if (uuid == null) {
                        uuid = UUID.randomUUID();
                        add.put(uuid, group);
                    }
                    return uuid;
                });
                playerToGroups.compute(player, (player1, nearbyGroups1) -> {
                    nearbyGroups1 = nearbyGroups1 == null ? new HashSet<>() : nearbyGroups1;
                    nearbyGroups1.add(group);
                    return nearbyGroups1;
                });
            } else {
                UUID uuid = nearbyGroups.get(group);
                if (uuid != null) {
                    remove.put(uuid, group);
                    playerToGroups.compute(player, (player1, nearbyGroups1) -> {
                        nearbyGroups1 = nearbyGroups1 == null ? new HashSet<>() : nearbyGroups1;
                        nearbyGroups1.remove(group);
                        return nearbyGroups1;
                    });
                }
            }
        });
        return new Result(add, remove);
    }

    /**
     * Returns the groups to leave
     * @param player
     * @return
     */
    public void removePlayerState(MinecraftPlayer player) {
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
