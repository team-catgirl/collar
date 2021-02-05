package team.catgirl.collar.protocol.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

/**
 * When sent with a null identity, prompts a login response
 */
public final class IdentifyRequest extends ProtocolRequest {
    @JsonCreator
    public IdentifyRequest(@JsonProperty("identity") ClientIdentity identity) {
        super(identity);
    }

    /**
     * @return identify request that will prompt the client to negotiate an identity with collar
     */
    public static IdentifyRequest unknown() {
        return new IdentifyRequest(null);
    }
}
