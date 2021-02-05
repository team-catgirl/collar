package team.catgirl.collar.protocol.signal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class SendPreKeysRequest extends ProtocolRequest {
    @JsonProperty("preKeyBundle")
    public final byte[] preKeyBundle;

    @JsonCreator
    public SendPreKeysRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("preKeyBundle") byte[] preKeyBundle) {
        super(identity);
        this.preKeyBundle = preKeyBundle;
    }
}
