package team.catgirl.collar.client.security.signal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.Objects;

final class StateKey {
    @JsonProperty("name")
    public final String name;
    @JsonProperty("deviceId")
    public final int deviceId;

    public StateKey(@JsonProperty("name") String name, @JsonProperty("deviceId") int deviceId) {
        this.name = name;
        this.deviceId = deviceId;
    }

    public static StateKey from(SignalProtocolAddress address) {
        return new StateKey(address.getName(), address.getDeviceId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateKey that = (StateKey) o;
        return deviceId == that.deviceId &&
                name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, deviceId);
    }
}
