package team.catgirl.collar.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import team.catgirl.collar.security.ClientIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "t")
public abstract class ProtocolRequest {
    @JsonProperty("identity")
    public final ClientIdentity identity;

    @JsonCreator
    public ProtocolRequest(@JsonProperty("identity") ClientIdentity identity) {
        this.identity = identity;
    }
}
