package team.catgirl.collar.protocol.sdht;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.sdht.events.AbstractSDHTEvent;
import team.catgirl.collar.security.ClientIdentity;

public final class SDHTEventRequest extends ProtocolRequest {
    @JsonProperty("event")
    public final AbstractSDHTEvent event;

    public SDHTEventRequest(@JsonProperty("identity") ClientIdentity identity,
                            @JsonProperty("event") AbstractSDHTEvent event) {
        super(identity);
        this.event = event;
    }
}
