package team.catgirl.collar.server.services.devices;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class Device {
    @JsonProperty("owner")
    public final UUID owner;
    @JsonProperty("deviceId")
    public final int deviceId;
    @JsonProperty("deviceName")
    public final String name;

    public Device(
            @JsonProperty("owner") UUID owner,
            @JsonProperty("deviceId") int deviceId,
            @JsonProperty("deviceName") String name) {
        this.owner = owner;
        this.deviceId = deviceId;
        this.name = name;
    }
}
