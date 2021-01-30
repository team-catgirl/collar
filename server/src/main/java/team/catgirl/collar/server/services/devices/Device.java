package team.catgirl.collar.server.services.devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.KeyPair.PublicKey;

import java.util.UUID;

public final class Device {
    @JsonProperty("profileId")
    public final UUID profileId;
    @JsonProperty("deviceId")
    public final int deviceId;
    @JsonProperty("deviceName")
    public final String name;
    @JsonProperty("publicKey")
    public final PublicKey publicKey;
    @JsonProperty("trusted")
    public final Boolean trusted;

    public Device(
            @JsonProperty("profileId") UUID profileId,
            @JsonProperty("deviceId") int deviceId,
            @JsonProperty("deviceName") String name,
            @JsonProperty("publicKey") PublicKey publicKey,
            @JsonProperty("trusted") Boolean trusted) {
        this.profileId = profileId;
        this.deviceId = deviceId;
        this.name = name;
        this.publicKey = publicKey;
        this.trusted = trusted;
    }
}
