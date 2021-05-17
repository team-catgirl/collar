package team.catgirl.collar.protocol.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public final class GetProfileResponse extends ProtocolResponse {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("profile")
    public final PublicProfile profile;

    public GetProfileResponse(@JsonProperty("identity") ServerIdentity identity,
                              @JsonProperty("id") UUID id,
                              @JsonProperty("profile") PublicProfile profile) {
        super(identity);
        this.id = id;
        this.profile = profile;
    }
}
