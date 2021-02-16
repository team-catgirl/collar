package team.catgirl.collar.protocol.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

public final class CreateTrustResponse extends ProtocolResponse {
    @JsonProperty("id")
    public final Long id;
    @JsonProperty("preKeyBundle")
    public final byte[] preKeyBundle;
    @JsonProperty("sender")
    public final ClientIdentity sender;

    @JsonCreator
    public CreateTrustResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("id") Long id,
                               @JsonProperty("preKeyBundle") byte[] preKeyBundle,
                               @JsonProperty("sender") ClientIdentity sender) {
        super(identity);
        this.id = id;
        this.preKeyBundle = preKeyBundle;
        this.sender = sender;
    }
}
