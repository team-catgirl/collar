package team.catgirl.collar.protocol.devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public class DeviceRegisteredResponse extends ProtocolResponse {
    @JsonProperty("deviceId")
    public Integer deviceId;

    public DeviceRegisteredResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("deviceId") Integer deviceId) {
        super(identity);
        this.deviceId = deviceId;
    }
}
