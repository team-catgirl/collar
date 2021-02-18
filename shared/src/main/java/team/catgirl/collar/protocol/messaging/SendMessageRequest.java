package team.catgirl.collar.protocol.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class SendMessageRequest extends ProtocolRequest {
    @JsonProperty("individual")
    public final ClientIdentity individual;
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("message")
    public final byte[] message;

    public SendMessageRequest(@JsonProperty("identity") ClientIdentity identity,
                              @JsonProperty("individual") ClientIdentity individual,
                              @JsonProperty("group") UUID group,
                              @JsonProperty("message") byte[] message) {
        super(identity);
        this.individual = individual;
        this.group = group;
        this.message = message;
    }
}
