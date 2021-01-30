package team.catgirl.collar.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.ServerIdentity;

public final class KeepAliveResponse extends ProtocolResponse {
    public KeepAliveResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }
}
