package team.catgirl.collar.protocol.signal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public final class SendPreKeysResponse extends ProtocolResponse {
    @JsonProperty("preKeyBundle")
    public final byte[] preKeyBundle;

    @JsonCreator
    public SendPreKeysResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("preKeyBundle") byte[] preKeyBundle) {
        super(identity);
        this.preKeyBundle = preKeyBundle;
    }
}
