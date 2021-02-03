package team.catgirl.collar.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import team.catgirl.collar.security.ClientIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "t")
public abstract class ProtocolRequest {
    @JsonProperty("identity")
    public final ClientIdentity identity;

    public ProtocolRequest(@JsonProperty("identity") ClientIdentity identity) {
        this.identity = identity;
    }
}
