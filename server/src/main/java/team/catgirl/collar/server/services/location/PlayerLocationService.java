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
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.services.profiles.ProfileService.GetProfileRequest;
import team.catgirl.collar.server.session.SessionManager;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlayerLocationService {

    private static final Logger LOGGER = Logger.getLogger(PlayerLocationService.class.getName());

    private final SessionManager sessions;
    private final ProfileService profiles;
    private final GroupService groups;
    private final ServerIdentity serverIdentity;
    private final NearbyGroups nearbyGroups = new NearbyGroups();

    private final ArrayListMultimap<UUID, Player> playersSharing = ArrayListMultimap.create();

    public PlayerLocationService(SessionManager sessions, ProfileService profiles, GroupService groups, ServerIdentity serverIdentity) {
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

    public BatchProtocolResponse stopSharing(StopSharingLocationRequest req) {
        Player player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player " + req.identity));
        return stopSharing(req.groupId, req.identity, player);
    }

    public BatchProtocolResponse stopSharing(Player player) {
        ClientIdentity identity = sessions.getIdentity(player).orElseThrow(() -> new IllegalStateException("could not find session for " + player));
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        List<BatchProtocolResponse> allResponses = playersSharing.asMap().entrySet().stream()
                .filter(candidate -> candidate.getValue().contains(player))
                .map(Map.Entry::getKey)
                .map(uuid -> stopSharing(uuid, identity, player))
                .collect(Collectors.toList());
        for (BatchProtocolResponse response : allResponses) {
            responses.concat(response);
        }
        return responses;
    }

    /**
     * Updates other players with the senders location if they have sharing enabled for these groups
     * @param req of the location
     * @return {@link LocationUpdatedResponse} responses to send to clients
     */
    public BatchProtocolResponse updateLocation(UpdateLocationRequest req) {
        Player player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player " + req.identity));
        return createLocationResponses(player, new LocationUpdatedResponse(serverIdentity, req.identity, req.group, player, req.location));
    }

    private BatchProtocolResponse stopSharing(UUID groupId, ClientIdentity identity, Player player) {
        LOGGER.log(Level.INFO,"Player " + player + " started sharing location with group " + groupId);
        LocationUpdatedResponse locationUpdatedResponse = new LocationUpdatedResponse(serverIdentity, identity, groupId, player, null);
        BatchProtocolResponse responses = createLocationResponses(player, locationUpdatedResponse);
        synchronized (playersSharing) {
            playersSharing.remove(groupId, player);
        }
        return responses;
    }

    public BatchProtocolResponse updateNearbyGroups(UpdateNearbyRequest req) {
        Player player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player " + req.identity));
        PublicProfile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(player.profile)).profile.toPublic();
        NearbyGroups.Result result = this.nearbyGroups.updateNearbyGroups(new MemberSource(player, profile), req.nearbyHashes);
        return groups.updateNearbyGroups(result);
    }

    private BatchProtocolResponse createLocationResponses(Player player, LocationUpdatedResponse resp) {
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
                uniquePlayers.add(memberPlayer);
                ClientIdentity identity = sessions.getIdentity(memberPlayer).orElseThrow(() -> new IllegalStateException("Could not find identity for player " + player));
                responses.add(identity, resp);
            }
        }
        return responses;
    }

    public void removePlayerState(Player player) {
        this.nearbyGroups.removePlayerState(player);
    }
}
