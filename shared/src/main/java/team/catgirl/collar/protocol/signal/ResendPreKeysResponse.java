package team.catgirl.collar.protocol.signal;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

/**
 * Used to get the client to re-exchange keys by sending a {@link SendPreKeysRequest} to the server
 */
public final class ResendPreKeysResponse extends ProtocolResponse {
    public ResendPreKeysResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }
}
