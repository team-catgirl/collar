package team.catgirl.collar.protocol.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

/**
 * Used to lookup a {@link ClientIdentity}
 */
public final class GetIdentityRequest extends ProtocolRequest {
    /**
     * Request identifier
     */
    public final Long id;

    /**
     * Player id to map to an identity
     */
    public final UUID player;

    public GetIdentityRequest(@JsonProperty("identity") ClientIdentity identity,
                              @JsonProperty("id") Long id,
                              @JsonProperty("player") UUID player) {
        super(identity);
        this.id = id;
        this.player = player;
    }
}
