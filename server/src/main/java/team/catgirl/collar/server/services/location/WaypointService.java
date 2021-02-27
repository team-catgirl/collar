package team.catgirl.collar.server.services.location;

import team.catgirl.collar.api.waypoints.EncryptedWaypoint;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.GetWaypointsRequest;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.services.profiles.storage.ProfileStorage;
import team.catgirl.collar.server.services.profiles.storage.ProfileStorage.Blob;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages private waypoints
 */
public final class WaypointService {
    private final ProfileStorage storage;
    private final ServerIdentity serverIdentity;

    public WaypointService(ProfileStorage storage, ServerIdentity serverIdentity) {
        this.storage = storage;
        this.serverIdentity = serverIdentity;
    }

    public void createWaypoint(CreateWaypointRequest req) {
        storage.store(req.identity.owner, req.waypointId, req.waypoint, 'W');
    }

    public void removeWaypoint(RemoveWaypointRequest req) {
        storage.delete(req.identity.owner, req.waypointId);
    }

    public List<EncryptedWaypoint> getWaypoints(GetWaypointsRequest req) {
        List<Blob> blobs = storage.find(req.identity.owner, 'W');
        return blobs.stream().map(blob -> new EncryptedWaypoint(blob.key, blob.data)).collect(Collectors.toList());
    }
}
