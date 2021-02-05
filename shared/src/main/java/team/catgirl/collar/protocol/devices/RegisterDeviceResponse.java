package team.catgirl.collar.protocol.devices;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

/**
 * Fired after the {@link RegisterDeviceRequest} is made with the client, with a URL to approve the device
 */
public final class RegisterDeviceResponse extends ProtocolResponse {
    @JsonProperty("approvalUrl")
    public final String approvalUrl;

    @JsonCreator
    public RegisterDeviceResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("approvalUrl") String approvalUrl) {
        super(identity);
        this.approvalUrl = approvalUrl;
    }
}
