package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

public final class TransferGroupOwnershipRequest extends ProtocolRequest {
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("profile")
    public final UUID profile;

    public TransferGroupOwnershipRequest(@JsonProperty("identity") ClientIdentity identity,
                                         @JsonProperty("group") UUID group,
                                         @JsonProperty("profile") UUID profile) {
        super(identity);
        this.group = group;
        this.profile = profile;
    }
}
