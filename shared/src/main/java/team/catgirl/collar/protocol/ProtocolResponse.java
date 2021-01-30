package team.catgirl.collar.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import team.catgirl.collar.security.ServerIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
public abstract class ProtocolResponse {
    @JsonProperty("identity")
    public final ServerIdentity identity;

    public ProtocolResponse(ServerIdentity identity) {
        this.identity = identity;
    }
}
