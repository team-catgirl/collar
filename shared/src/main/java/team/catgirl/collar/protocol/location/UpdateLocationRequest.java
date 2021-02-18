package team.catgirl.collar.protocol.location;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

/**
 * Sent by the client to update the players current location
 * There is no response sent back to the sender for this request as this would be too noisy.
 */
public final class UpdateLocationRequest extends ProtocolRequest {
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("location")
    public final byte[] location;

    @JsonCreator
    public UpdateLocationRequest(@JsonProperty("identity") ClientIdentity identity,
                                 @JsonProperty("group") UUID group,
                                 @JsonProperty("location") byte[] location) {
        super(identity);
        this.group = group;
        this.location = location;
    }
}
