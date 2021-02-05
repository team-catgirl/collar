package team.catgirl.collar.protocol.trust;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public abstract class CheckTrustRelationshipResponse extends ProtocolResponse {
    @JsonCreator
    public CheckTrustRelationshipResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }

    public static final class IsTrustedRelationshipResponse extends CheckTrustRelationshipResponse {
        @JsonCreator
        public IsTrustedRelationshipResponse(@JsonProperty("identity") ServerIdentity identity) {
            super(identity);
        }
    }

    public static final class IsUntrustedRelationshipResponse extends CheckTrustRelationshipResponse {
        @JsonCreator
        public IsUntrustedRelationshipResponse(@JsonProperty("identity") ServerIdentity identity) {
            super(identity);
        }
    }
}
