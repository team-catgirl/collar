package team.catgirl.collar.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.ServerIdentity;

/**
 * Fired after the {@link RegisterDeviceRequest} is made with the client, with a URL to approve the device
 */
public class RegisterDeviceResponse extends ProtocolResponse {
    @JsonProperty("approvalUrl")
    public final String approvalUrl;

    public RegisterDeviceResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("approvalUrl") String approvalUrl) {
        super(identity);
        this.approvalUrl = approvalUrl;
    }
}
