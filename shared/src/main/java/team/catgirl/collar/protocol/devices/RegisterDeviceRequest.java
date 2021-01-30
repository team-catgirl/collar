package team.catgirl.collar.protocol.devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.PlayerIdentity;

public class RegisterDeviceRequest extends ProtocolRequest {
    public RegisterDeviceRequest(@JsonProperty("playerIdentity") PlayerIdentity identity) {
        super(identity);
    }
}
