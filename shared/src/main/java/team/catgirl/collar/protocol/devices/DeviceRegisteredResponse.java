package team.catgirl.collar.protocol.devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public final class DeviceRegisteredResponse extends ProtocolResponse {
    @JsonProperty("profile")
    public final PublicProfile profile;
    @JsonProperty("deviceId")
    public final Integer deviceId;

    public DeviceRegisteredResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("profile") PublicProfile profile, @JsonProperty("deviceId") Integer deviceId) {
        super(identity);
        this.profile = profile;
        this.deviceId = deviceId;
    }
}
