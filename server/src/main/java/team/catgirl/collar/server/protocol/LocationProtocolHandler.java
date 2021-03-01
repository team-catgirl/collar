package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.api.waypoints.EncryptedWaypoint;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.location.StartSharingLocationRequest;
import team.catgirl.collar.protocol.location.StopSharingLocationRequest;
import team.catgirl.collar.protocol.location.UpdateLocationRequest;
import team.catgirl.collar.protocol.location.UpdateNearbyRequest;
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.GetWaypointsRequest;
import team.catgirl.collar.protocol.waypoints.GetWaypointsResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.services.location.PlayerLocationService;
import team.catgirl.collar.server.services.location.WaypointService;

import java.util.List;
import java.util.function.BiConsumer;

public class LocationProtocolHandler extends ProtocolHandler {

    private final PlayerLocationService playerLocations;
    private final WaypointService waypoints;
    private final ServerIdentity serverIdentity;

    public LocationProtocolHandler(PlayerLocationService playerLocations, WaypointService waypoints, ServerIdentity serverIdentity) {
        this.playerLocations = playerLocations;
        this.waypoints = waypoints;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof StartSharingLocationRequest) {
            StartSharingLocationRequest request = (StartSharingLocationRequest)req;
            playerLocations.startSharing(request);
            return true;
        } else if (req instanceof StopSharingLocationRequest) {
            StopSharingLocationRequest request = (StopSharingLocationRequest) req;
            BatchProtocolResponse resp = playerLocations.stopSharing(request);
            sender.accept(request.identity, resp);
            return true;
        } else if (req instanceof UpdateLocationRequest) {
            UpdateLocationRequest request = (UpdateLocationRequest) req;
            BatchProtocolResponse resp = playerLocations.updateLocation(request);
            sender.accept(request.identity, playerLocations.updateLocation(request));
            return true;
        } else if (req instanceof UpdateNearbyRequest) {
            UpdateNearbyRequest request = (UpdateNearbyRequest) req;
            BatchProtocolResponse resp = playerLocations.updateNearbyGroups(request);
            sender.accept(null, resp);
            return true;
        } else if (req instanceof CreateWaypointRequest) {
            CreateWaypointRequest request = (CreateWaypointRequest) req;
            waypoints.createWaypoint(request);
            return true;
        } else if (req instanceof RemoveWaypointRequest) {
            RemoveWaypointRequest request = (RemoveWaypointRequest) req;
            waypoints.removeWaypoint(request);
            return true;
        } else if (req instanceof GetWaypointsRequest) {
            GetWaypointsRequest request = (GetWaypointsRequest) req;
            List<EncryptedWaypoint> waypoints = this.waypoints.getWaypoints(request);
            sender.accept(req.identity, new GetWaypointsResponse(serverIdentity, waypoints));
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {
        BatchProtocolResponse resp = playerLocations.stopSharing(player);
        sender.accept(null, resp);
        playerLocations.removePlayerState(player);
    }
}
