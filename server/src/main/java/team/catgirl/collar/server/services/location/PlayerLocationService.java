package team.catgirl.collar.server.services.location;

import com.google.common.collect.ArrayListMultimap;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.protocol.location.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.session.SessionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlayerLocationService {

    private static final Logger LOGGER = Logger.getLogger(PlayerLocationService.class.getName());

    private final SessionManager sessions;
    private final GroupService groups;
    private final ServerIdentity serverIdentity;
    private final ConcurrentHashMap<NearbyKey, NearbyRecord> nearbyRecords = new ConcurrentHashMap<>();

    private final ArrayListMultimap<UUID, MinecraftPlayer> playersSharing = ArrayListMultimap.create();

    public PlayerLocationService(SessionManager sessions, GroupService groups, ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.groups = groups;
        this.serverIdentity = serverIdentity;
    }

    public void startSharing(StartSharingLocationRequest req) {
        sessions.findPlayer(req.identity).ifPresent(player -> {
            synchronized (playersSharing) {
                playersSharing.put(req.groupId, player);
            }
            LOGGER.log(Level.INFO,"Player " + player + " started sharing location with group " + req.groupId);
        });
    }

    public BatchProtocolResponse stopSharing(StopSharingLocationRequest req) {
        MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player " + req.identity));
        return stopSharing(req.groupId, req.identity, player);
    }

    public BatchProtocolResponse stopSharing(MinecraftPlayer player) {
        ClientIdentity identity = sessions.getIdentity(player).orElseThrow(() -> new IllegalStateException("could not find session for " + player));
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        List<BatchProtocolResponse> allResponses = playersSharing.asMap().entrySet().stream()
                .filter(candidate -> candidate.getValue().contains(player))
                .map(Map.Entry::getKey)
                .map(uuid -> stopSharing(uuid, identity, player))
                .collect(Collectors.toList());;
        for (BatchProtocolResponse response : allResponses) {
            responses = responses.concat(response);
        }
        return responses;
    }

    /**
     * Updates other players with the senders location if they have sharing enabled for these groups
     * @param req of the location
     * @return {@link LocationUpdatedResponse} responses to send to clients
     */
    public BatchProtocolResponse updateLocation(UpdateLocationRequest req) {
        MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player " + req.identity));
        return createLocationResponses(player, new LocationUpdatedResponse(serverIdentity, req.identity, req.group, player, req.location));
    }

    private BatchProtocolResponse stopSharing(UUID groupId, ClientIdentity identity, MinecraftPlayer player) {
        LOGGER.log(Level.INFO,"Player " + player + " started sharing location with group " + groupId);
        LocationUpdatedResponse locationUpdatedResponse = new LocationUpdatedResponse(serverIdentity, identity, groupId, player, null);
        BatchProtocolResponse responses = createLocationResponses(player, locationUpdatedResponse);
        synchronized (playersSharing) {
            playersSharing.remove(groupId, player);
        }
        return responses;
    }

    public BatchProtocolResponse updateNearbyGroups(UpdateNearbyRequest req) {
        MinecraftPlayer player = sessions.findPlayer(req.identity)
                .orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));
        Map<String, Set<MinecraftPlayer>> entityPresenceUpdates = new HashMap<>();
        nearbyRecords.keySet().forEach(nearbyKey -> {
            nearbyRecords.compute(nearbyKey, (key, record) -> {
                if (req.nearbyHashes.contains(key.nearbyHash)) {
                    if (record == null) {
                        return new NearbyRecord(player.server, key.nearbyHash, Set.of(player.id));
                    } else {
                        Set<UUID> playersReporting = new HashSet<>(record.playersSharingHash);
                        playersReporting.add(player.id);
                        entityPresenceUpdates.put(nearbyKey.nearbyHash, playersReporting.stream().map(uuid -> new MinecraftPlayer(uuid, key.server)).collect(Collectors.toSet()));
                        return new NearbyRecord(player.server, nearbyKey.nearbyHash, playersReporting);
                    }
                } else {
                    if (record == null) {
                        return null;
                    } else {
                        Set<UUID> playersReporting = new HashSet<>(record.playersSharingHash);
                        playersReporting.remove(player.id);
                        entityPresenceUpdates.put(nearbyKey.nearbyHash, playersReporting.stream().map(uuid -> new MinecraftPlayer(uuid, key.server)).collect(Collectors.toSet()));
                        return new NearbyRecord(player.server, nearbyKey.nearbyHash, playersReporting);
                    }
                }
            });
        });
        return groups.updateNearbyGroups(entityPresenceUpdates);
    }

    private BatchProtocolResponse createLocationResponses(MinecraftPlayer player, LocationUpdatedResponse resp) {
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        // Find all the groups the requesting player is a member of
        List<UUID> sharingWithGroups = playersSharing.entries().stream().filter(entry -> player.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<Group> memberGroups = groups.findGroups(sharingWithGroups);
        // Keep track of players we have sent to, so we do not send them duplicate messages (e.g. if they share membership of 2 or more groups)
        HashSet<MinecraftPlayer> uniquePlayers = new HashSet<>();
        for (Group group : memberGroups) {
            for (Map.Entry<MinecraftPlayer, Member> entry : group.members.entrySet()) {
                MinecraftPlayer memberPlayer = entry.getKey();
                // Do not send to self
                if (memberPlayer.equals(player)) {
                    continue;
                }
                Member member = entry.getValue();
                if (uniquePlayers.contains(memberPlayer) || member.membershipState != Group.MembershipState.ACCEPTED) {
                    continue;
                }
                uniquePlayers.add(memberPlayer);
                ClientIdentity identity = sessions.getIdentity(memberPlayer).orElseThrow(() -> new IllegalStateException("Could not find identity for player " + player));
                responses = responses.add(identity, resp);
            }
        }
        return responses;
    }

    public static final class NearbyKey {
        public final String server;
        public final String nearbyHash;

        public NearbyKey(String server, String nearbyHash) {
            this.server = server;
            this.nearbyHash = nearbyHash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NearbyKey nearbyKey = (NearbyKey) o;
            return server.equals(nearbyKey.server) && nearbyHash.equals(nearbyKey.nearbyHash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(server, nearbyHash);
        }
    }

    public static final class NearbyRecord {
        public final String hash;
        public final Set<UUID> playersSharingHash;

        public NearbyRecord(String server, String hash, Set<UUID> playersSharingHash) {
            this.hash = hash;
            this.playersSharingHash = playersSharingHash;
        }
    }
}
