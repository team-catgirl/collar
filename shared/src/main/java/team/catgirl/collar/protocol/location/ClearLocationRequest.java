package team.catgirl.collar.protocol.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

/**
 * Sent by a client when it wants to stop sharing its location
 */
public final class ClearLocationRequest extends ProtocolRequest {
    public ClearLocationRequest(@JsonProperty("identity") ClientIdentity identity) {
        super(identity);
    }
}
