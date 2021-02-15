package team.catgirl.collar.protocol.signal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

/**
 * Alice sends, via the Server, {@link SendPreKeysRequest}
 * The server receives the request and translates it into a {@link ExchangePreKeysResponse} and sends this to Bob.
 * Bob creates a session
 * Bob sends back, via the server, a {@link SendPreKeysRequest} addressed to Alice
 * Alice creates a session
 * The two parties can now crypt messages to each other
 */
public final class ExchangePreKeysResponse extends ProtocolResponse {
    public final Long id;
    @JsonProperty("preKeyBundle")
    public final byte[] preKeyBundle;
    @JsonProperty("owner")
    public final ClientIdentity owner;

    @JsonCreator
    public ExchangePreKeysResponse(@JsonProperty("identity") ServerIdentity identity,
                                   @JsonProperty("identity") Long id,
                                   @JsonProperty("preKeyBundle") byte[] preKeyBundle,
                                   @JsonProperty("owner") ClientIdentity owner) {
        super(identity);
        this.id = id;
        this.preKeyBundle = preKeyBundle;
        this.owner = owner;
    }
}
