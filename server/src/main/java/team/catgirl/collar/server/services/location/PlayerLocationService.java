package team.catgirl.collar.server.services.location;

import com.google.common.collect.ArrayListMultimap;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Member;
import team.catgirl.collar.api.groups.MemberSource;
import team.catgirl.collar.api.groups.MembershipState;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.location.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.services.profiles.ProfileCache;
import team.catgirl.collar.server.session.SessionManager;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlayerLocationService {

    private static final Logger LOGGER = Logger.getLogger(PlayerLocationService.class.getName());

    private final SessionManager sessions;
    private final ProfileCache profiles;
    private final GroupService groups;
    private final ServerIdentity serverIdentity;
    private final NearbyGroups nearbyGroups = new NearbyGroups();

    private final ArrayListMultimap<UUID, Player> playersSharing = ArrayListMultimap.create();

    public PlayerLocationService(SessionManager sessions, ProfileCache profiles, GroupService groups, ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.profiles = profiles;
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

    public Optional<BatchProtocolResponse> stopSharing(StopSharingLocationRequest req) {
        return sessions.findPlayer(req.identity)
                .flatMap(player -> stopSharing(req.groupId, req.identity, player));
    }

    public Optional<BatchProtocolResponse> stopSharing(Player player) {
        return sessions.getIdentity(player)
                .map(identity -> {
                    BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
                    List<BatchProtocolResponse> allResponses = playersSharing.asMap().entrySet().stream()
                            .filter(candidate -> candidate.getValue().contains(player))
                            .map(Map.Entry::getKey)
                            .map(uuid -> stopSharing(uuid, identity, player))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList());

                    for (BatchProtocolResponse response : allResponses) {
                        responses.concat(response);
                    }
                    return responses;
        });
    }

    /**
     * Updates other players with the senders location if they have sharing enabled for these groups
     * @param req of the location
     * @return {@link LocationUpdatedResponse} responses to send to clients
     */
    public Optional<BatchProtocolResponse> updateLocation(UpdateLocationRequest req) {
        return sessions.findPlayer(req.identity).flatMap(player -> createLocationResponses(player, new LocationUpdatedResponse(serverIdentity, req.identity, req.group, player, req.location)));
    }

    private Optional<BatchProtocolResponse> stopSharing(UUID groupId, ClientIdentity identity, Player player) {
        LOGGER.log(Level.INFO,"Player " + player + " started sharing location with group " + groupId);
        LocationUpdatedResponse locationUpdatedResponse = new LocationUpdatedResponse(serverIdentity, identity, groupId, player, null);
        Optional<BatchProtocolResponse> responses = createLocationResponses(player, locationUpdatedResponse);
        synchronized (playersSharing) {
            playersSharing.remove(groupId, player);
        }
        return responses;
    }

    public Optional<BatchProtocolResponse> updateNearbyGroups(UpdateNearbyRequest req) {
        Optional<Player> player = sessions.findPlayer(req.identity);
        if (player.isEmpty()) {
            return Optional.empty();
        }
        PublicProfile profile = profiles.getById(player.get().profile).orElseThrow(() -> new IllegalStateException("could not find profile " + player.get().profile)).toPublic();
        NearbyGroups.Result result = this.nearbyGroups.updateNearbyGroups(new MemberSource(player.get(), profile), req.nearbyHashes);
        return groups.updateNearbyGroups(result);
    }

    private Optional<BatchProtocolResponse> createLocationResponses(Player player, LocationUpdatedResponse resp) {
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        // Find all the groups the requesting player is a member of
        Set<UUID> sharingWithGroups = playersSharing.entries().stream().filter(entry -> player.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<Group> memberGroups = groups.findGroups(sharingWithGroups);
        // Keep track of players we have sent to, so we do not send them duplicate messages (e.g. if they share membership of 2 or more groups)
        HashSet<Player> uniquePlayers = new HashSet<>();
        for (Group group : memberGroups) {
            for (Member member : group.members) {
                Player memberPlayer = member.player;
                // Do not send to self
                if (memberPlayer.equals(player)) {
                    continue;
                }
                if (uniquePlayers.contains(memberPlayer) || member.membershipState != MembershipState.ACCEPTED) {
                    continue;
                }
                sessions.getIdentity(memberPlayer).ifPresent(identity -> {
                    uniquePlayers.add(memberPlayer);
                    responses.add(identity, resp);
                });
            }
        }
        return responses.optional();
    }

    public void removePlayerState(Player player) {
        this.nearbyGroups.removePlayerState(player);
    }
}
