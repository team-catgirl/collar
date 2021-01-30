package team.catgirl.collar.protocol.trust;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public abstract class CheckTrustRelationshipResponse extends ProtocolResponse {
    public CheckTrustRelationshipResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }

    public static final class IsTrustedRelationshipResponse extends CheckTrustRelationshipResponse {
        public IsTrustedRelationshipResponse(@JsonProperty("identity") ServerIdentity identity) {
            super(identity);
        }
    }

    public static final class IsUntrustedRelationshipResponse extends CheckTrustRelationshipResponse {
        public IsUntrustedRelationshipResponse(@JsonProperty("identity") ServerIdentity identity) {
            super(identity);
        }
    }
}
