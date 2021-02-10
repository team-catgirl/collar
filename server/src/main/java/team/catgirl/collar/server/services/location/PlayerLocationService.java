package team.catgirl.collar.server.services.location;

import org.eclipse.jetty.util.log.Log;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Group.Member;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.protocol.location.LocationUpdatedResponse;
import team.catgirl.collar.protocol.location.StartSharingLocationRequest;
import team.catgirl.collar.protocol.location.StopSharingLocationRequest;
import team.catgirl.collar.protocol.location.UpdateLocationRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.session.SessionManager;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlayerLocationService {

    private static final Logger LOGGER = Logger.getLogger(PlayerLocationService.class.getName());

    private final SessionManager sessions;
    private final GroupService groups;
    private final ServerIdentity serverIdentity;

    private final ConcurrentHashMap<UUID, MinecraftPlayer> playersSharing = new ConcurrentHashMap<>();

    public PlayerLocationService(SessionManager sessions, GroupService groups, ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.groups = groups;
        this.serverIdentity = serverIdentity;
    }

    public void startSharing(StartSharingLocationRequest req) {
        sessions.findPlayer(req.identity).map(player -> playersSharing.put(req.groupId, player)).ifPresent(player -> {
            LOGGER.log(Level.INFO,"Player " + player + " started sharing location with group " + req.groupId);
        });
    }

    public BatchProtocolResponse stopSharing(StopSharingLocationRequest req) {
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        sessions.findPlayer(req.identity).ifPresent(player -> {
            stopSharing(req.groupId, req.identity, responses, player);
        });
        return responses;
    }

    public BatchProtocolResponse stopSharing(MinecraftPlayer player) {
        BatchProtocolResponse responses = new BatchProtocolResponse(serverIdentity);
        playersSharing.entrySet().stream()
                .filter(candidate -> candidate.getValue().equals(player))
                .map(Map.Entry::getKey)
                .forEach(uuid -> {
                    sessions.getIdentity(player).ifPresent(identity -> {
                        stopSharing(uuid, identity, responses, player);
                    });
                });
        return responses;
    }

    /**
     * Updates other players with the senders location if they have sharing enabled for these groups
     * @param req of the location
     * @return {@link LocationUpdatedResponse} responses to send to clients
     */
    public BatchProtocolResponse updateLocation(UpdateLocationRequest req) {
        BatchProtocolResponse response = new BatchProtocolResponse(serverIdentity);
        sessions.findPlayer(req.identity).ifPresent(player -> {
            LocationUpdatedResponse locationUpdatedResponse = new LocationUpdatedResponse(serverIdentity, req.identity, player, req.location);
            createLocationResponses(response, player, locationUpdatedResponse);
        });
        return response;
    }

    private void stopSharing(UUID groupId, ClientIdentity identity, BatchProtocolResponse responses, MinecraftPlayer player) {
        LOGGER.log(Level.INFO,"Player " + player + " started sharing location with group " + groupId);
        LocationUpdatedResponse locationUpdatedResponse = new LocationUpdatedResponse(serverIdentity, identity, player, Location.UNKNOWN);
        createLocationResponses(responses, player, locationUpdatedResponse);
        playersSharing.remove(groupId);
    }

    private void createLocationResponses(BatchProtocolResponse responses, MinecraftPlayer player, LocationUpdatedResponse resp) {
        // Find all the groups the requesting player is a member of
        List<UUID> sharingWithGroups = playersSharing.entrySet().stream().filter(entry -> player.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<Group> memberGroups = groups.findGroups(sharingWithGroups);
        // Keep track of players we have sent to, so we do not send them duplicate messages (e.g. if they share membership of 2 or more groups)
        HashSet<MinecraftPlayer> uniquePlayers = new HashSet<>();
        for (Group group : memberGroups) {
            for (Map.Entry<MinecraftPlayer, Member> entry : group.members.entrySet()) {
                MinecraftPlayer memberPlayer = entry.getKey();
                if (memberPlayer.equals(player)) {
                    continue;
                }
                Member member = entry.getValue();
                if (uniquePlayers.contains(memberPlayer) || member.membershipState != Group.MembershipState.ACCEPTED) {
                    continue;
                }
                uniquePlayers.add(memberPlayer);
                sessions.getIdentity(memberPlayer).ifPresent(clientIdentity -> {
                    responses.add(clientIdentity, resp);
                });
            }
        }
    }
}
