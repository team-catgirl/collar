package team.catgirl.collar.protocol.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class GetProfileRequest extends ProtocolRequest {
    @JsonProperty("profile")
    public final UUID profile;

    public GetProfileRequest(@JsonProperty("identify") ClientIdentity identity,
                             @JsonProperty("profile") UUID profile) {
        super(identity);
        this.profile = profile;
    }
}
