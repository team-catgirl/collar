package team.catgirl.collar.protocol.sdht;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.sdht.events.AbstractSDHTEvent;
import team.catgirl.collar.security.ServerIdentity;

public final class SDHTEventResponse extends ProtocolResponse {
    @JsonProperty("event")
    public final AbstractSDHTEvent event;

    public SDHTEventResponse(@JsonProperty("identity") ServerIdentity identity,
                             @JsonProperty("event") AbstractSDHTEvent event) {
        super(identity);
        this.event = event;
    }
}
