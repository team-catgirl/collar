package team.catgirl.collar.protocol.keepalive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public final class KeepAliveResponse extends ProtocolResponse {
    @JsonCreator
    public KeepAliveResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }
}
