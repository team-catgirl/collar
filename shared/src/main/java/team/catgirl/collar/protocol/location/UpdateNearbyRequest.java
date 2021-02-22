package team.catgirl.collar.protocol.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.Set;

public final class UpdateNearbyRequest extends ProtocolRequest {
    /**
     * sha256 hashes of player identity ids that the sender can see
     */
    public final Set<String> nearbyHashes;

    public UpdateNearbyRequest(@JsonProperty("identity") ClientIdentity identity,
                               @JsonProperty("nearbyHashes") Set<String> nearbyHashes) {
        super(identity);
        this.nearbyHashes = nearbyHashes;
    }
}
