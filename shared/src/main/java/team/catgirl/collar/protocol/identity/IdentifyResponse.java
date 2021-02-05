package team.catgirl.collar.protocol.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public final class IdentifyResponse extends ProtocolResponse {
    @JsonProperty("profile")
    public final PublicProfile profile;

    @JsonCreator
    public IdentifyResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("profile") PublicProfile profile) {
        super(identity);
        this.profile = profile;
    }
}
