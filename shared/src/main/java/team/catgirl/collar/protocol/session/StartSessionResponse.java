package team.catgirl.collar.protocol.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public final class StartSessionResponse extends ProtocolResponse {
    @JsonProperty("publicKey")
    final byte[] publicKey;

    @JsonCreator
    public StartSessionResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("publicKey") byte[] publicKey) {
        super(identity);
        this.publicKey = publicKey;
    }
}
