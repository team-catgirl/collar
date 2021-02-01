package team.catgirl.collar.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import team.catgirl.collar.security.ClientIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
public abstract class ProtocolRequest {
    @JsonProperty("identity")
    public final ClientIdentity identity;

    public ProtocolRequest(@JsonProperty("identity") ClientIdentity identity) {
        this.identity = identity;
    }
}
