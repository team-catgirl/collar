package team.catgirl.collar.server.services.location;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.protocol.groups.EjectGroupMemberRequest;
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
    private final NearbyGroups nearbyGroups = new NearbyGroups();

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
        MinecraftPlayer player = sessions.findPlayer(req.identity).orElseThrow(() -> new IllegalStateException("could not find player " + req.identity));
        NearbyGroups.Result result = this.nearbyGroups.updateNearbyGroups(player, req.nearbyHashes);
        return groups.updateNearbyGroups(result);
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
                responses.add(identity, resp);
            }
        }
        return responses;
    }

    public void removePlayerState(MinecraftPlayer player) {
        this.nearbyGroups.removePlayerState(player);
    }
}
