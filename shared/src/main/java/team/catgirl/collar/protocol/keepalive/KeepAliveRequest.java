package team.catgirl.collar.protocol.keepalive;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.PlayerIdentity;

public final class KeepAliveRequest extends ProtocolRequest {
    public KeepAliveRequest(@JsonProperty("identity") PlayerIdentity identity) {
        super(identity);
    }
}
