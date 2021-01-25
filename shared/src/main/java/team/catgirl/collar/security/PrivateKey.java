package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class PrivateKey {
    @JsonProperty("bytes")
    public final byte[] bytes;

    public PrivateKey(@JsonProperty("bytes") byte[] bytes) {
        this.bytes = bytes;
    }
}
