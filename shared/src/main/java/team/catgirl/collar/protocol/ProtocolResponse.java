package team.catgirl.collar.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import team.catgirl.collar.security.ServerIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "t")
public abstract class ProtocolResponse {
    @JsonProperty("identity")
    public final ServerIdentity identity;

    @JsonCreator
    public ProtocolResponse(ServerIdentity identity) {
        this.identity = identity;
    }
}
