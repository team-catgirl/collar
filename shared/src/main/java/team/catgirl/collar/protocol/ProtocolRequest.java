package team.catgirl.collar.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import team.catgirl.collar.security.PlayerIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
public abstract class ProtocolRequest {
    @JsonProperty("identity")
    public final PlayerIdentity identity;

    public ProtocolRequest(@JsonProperty("identity") PlayerIdentity identity) {
        this.identity = identity;
    }
}
