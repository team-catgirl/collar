package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class PublicKey {
    @JsonProperty("fingerPrint")
    public final byte[] fingerPrint;
    @JsonProperty("bytes")
    public final byte[] bytes;

    public PublicKey(@JsonProperty("fingerPrint") byte[] fingerPrint, @JsonProperty("bytes") byte[] bytes) {
        this.fingerPrint = fingerPrint;
        this.bytes = bytes;
    }
}
