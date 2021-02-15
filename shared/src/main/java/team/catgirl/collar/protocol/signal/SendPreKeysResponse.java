package team.catgirl.collar.protocol.signal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

public final class SendPreKeysResponse extends ProtocolResponse {
    @JsonProperty("id")
    public final Long id;
    @JsonProperty("preKeyBundle")
    public final byte[] preKeyBundle;
    @JsonProperty("owner")
    public final ClientIdentity owner;

    @JsonCreator
    public SendPreKeysResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("id") Long id,
                               @JsonProperty("preKeyBundle") byte[] preKeyBundle,
                               @JsonProperty("owner") ClientIdentity owner) {
        super(identity);
        this.id = id;
        this.preKeyBundle = preKeyBundle;
        this.owner = owner;
    }
}
