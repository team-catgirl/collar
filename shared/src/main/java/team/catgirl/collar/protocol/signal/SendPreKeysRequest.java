package team.catgirl.collar.protocol.signal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class SendPreKeysRequest extends ProtocolRequest {
    @JsonProperty("preKeyBundle")
    public final byte[] preKeyBundle;
    @JsonProperty("client")
    public final ClientIdentity recipient;

    @JsonCreator
    public SendPreKeysRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("preKeyBundle") byte[] preKeyBundle, @JsonProperty("recipient") ClientIdentity recipient) {
        super(identity);
        this.preKeyBundle = preKeyBundle;
        this.recipient = recipient;
    }
}
