package team.catgirl.collar.protocol.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public class FinishSessionHandshakeResponse extends ProtocolResponse {
    @JsonCreator
    public FinishSessionHandshakeResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }
}
