package team.catgirl.collar.protocol.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

/**
 * Create a trust relationship with another party
 * Alice sends a CreateTrustRequest addressed to Bob with a unique id to the server
 * Server converts the CreateTrustRequest into a CreateTrustResponse and sends it to Bob
 * Bob trusts the PreKeyBundle from Alice and responds with a CreateTrustRequest addressed to Alice using the same id provided by Alice
 * Server converts the CreateTrustRequest into a CreateTrustResponse and sends it to Alice
 * Alice checks the response and sees that it is her request ID, and notifies the client code that trust has been established
 */
public final class CreateTrustRequest extends ProtocolRequest {
    @JsonProperty("id")
    public final Long id;
    @JsonProperty("recipient")
    public final ClientIdentity recipient;
    @JsonProperty("preKeyBundle")
    public final byte[] preKeyBundle;

    public CreateTrustRequest(@JsonProperty("identity") ClientIdentity identity,
                              @JsonProperty("id") Long id,
                              @JsonProperty("recipient") ClientIdentity recipient,
                              @JsonProperty("preKeyBundle") byte[] preKeyBundle) {
        super(identity);
        this.id = id;
        this.recipient = recipient;
        this.preKeyBundle = preKeyBundle;
    }
}
