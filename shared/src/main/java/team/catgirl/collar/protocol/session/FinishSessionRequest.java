package team.catgirl.collar.protocol.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;

public class FinishSessionRequest extends ProtocolRequest {
    @JsonProperty("username")
    public String username;

    @JsonCreator
    public FinishSessionRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("username") String username) {
        super(identity);
        this.username=username;
    }
}