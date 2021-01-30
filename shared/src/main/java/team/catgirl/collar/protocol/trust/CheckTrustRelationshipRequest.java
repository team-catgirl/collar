package team.catgirl.collar.protocol.trust;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.PlayerIdentity;

public final class CheckTrustRelationshipRequest extends ProtocolRequest {
    public CheckTrustRelationshipRequest(@JsonProperty("identity") PlayerIdentity identity) {
        super(identity);
    }
}
