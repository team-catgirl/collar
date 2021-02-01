package team.catgirl.collar.api.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public final class CollarVersion {
    @JsonProperty("major")
    public final int major;
    @JsonProperty("minor")
    public final int minor;

    public CollarVersion(@JsonProperty("major") int major, @JsonProperty("minor") int minor) {
        this.major = major;
        this.minor = minor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollarVersion that = (CollarVersion) o;
        return major == that.major &&
                minor == that.minor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
