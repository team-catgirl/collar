package team.catgirl.collar.protocol.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class SendMessageRequest extends ProtocolRequest {
    @JsonProperty("recipient")
    public final ClientIdentity recipient;
    @JsonProperty("message")
    public final byte[] message;

    public SendMessageRequest(@JsonProperty("identity") ClientIdentity identity,
                              @JsonProperty("recipient") ClientIdentity recipient,
                              @JsonProperty("message") byte[] message) {
        super(identity);
        this.recipient = recipient;
        this.message = message;
    }
}
