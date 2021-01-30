package team.catgirl.collar.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.ServerIdentity;

public class KeepAliveResponse extends ProtocolResponse {
    public KeepAliveResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }
}
