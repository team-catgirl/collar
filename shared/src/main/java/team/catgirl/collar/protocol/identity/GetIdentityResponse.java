package team.catgirl.collar.protocol.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

public final class GetIdentityResponse extends ProtocolResponse {

    /**
     * Request identifier
     */
    public final Long id;

    /**
     * The player that was found
     */
    public final ClientIdentity found;

    public GetIdentityResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("id") Long id,
                               @JsonProperty("found") ClientIdentity found) {
        super(identity);
        this.id = id;
        this.found = found;
    }
}
