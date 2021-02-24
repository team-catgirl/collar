package team.catgirl.collar.protocol.profile;

import team.catgirl.collar.api.waypoints.EncryptedWaypoint;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public class UpdateProfileRequest extends ProtocolRequest {
    public final EncryptedWaypoint addWaypoint;

    public UpdateProfileRequest(ClientIdentity identity, EncryptedWaypoint addWaypoint) {
        super(identity);
        this.addWaypoint = addWaypoint;
    }
}
