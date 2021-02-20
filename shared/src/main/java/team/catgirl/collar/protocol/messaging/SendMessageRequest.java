package team.catgirl.collar.protocol.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class SendMessageRequest extends ProtocolRequest {
    /**
     * Client recipient
     */
    @JsonProperty("recipient")
    public final ClientIdentity recipient;

    /**
     * Group recipient
     */
    @JsonProperty("group")
    public final UUID group;

    /**
     * Crypted message
     */
    @JsonProperty("message")
    public final byte[] message;

    public SendMessageRequest(@JsonProperty("identity") ClientIdentity identity,
                              @JsonProperty("recipient") ClientIdentity recipient,
                              @JsonProperty("group") UUID group,
                              @JsonProperty("message") byte[] message) {
        super(identity);
        this.recipient = recipient;
        this.group = group;
        this.message = message;
    }
}
