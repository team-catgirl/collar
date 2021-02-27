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
    private static final String WAYPOINT_BLOB_TYPE = "W";

    private final ProfileStorage storage;

    public WaypointService(ProfileStorage storage) {
        this.storage = storage;
    }

    public void createWaypoint(CreateWaypointRequest req) {
        storage.store(req.identity.owner, req.waypointId, req.waypoint, WAYPOINT_BLOB_TYPE);
    }

    public void removeWaypoint(RemoveWaypointRequest req) {
        storage.delete(req.identity.owner, req.waypointId);
    }

    public List<EncryptedWaypoint> getWaypoints(GetWaypointsRequest req) {
        List<Blob> blobs = storage.find(req.identity.owner, WAYPOINT_BLOB_TYPE);
        return blobs.stream().map(blob -> new EncryptedWaypoint(blob.key, blob.data)).collect(Collectors.toList());
    }
}
