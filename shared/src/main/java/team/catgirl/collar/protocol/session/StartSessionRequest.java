package team.catgirl.collar.protocol.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.PlayerIdentity;

public final class StartSessionRequest extends ProtocolRequest {
    public StartSessionRequest(@JsonProperty("identity") PlayerIdentity identity) {
        super(identity);
    }
}
