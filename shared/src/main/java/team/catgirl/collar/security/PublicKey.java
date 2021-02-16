package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

/**
 * Represents a {@link Identity}'s public key
 */
public final class PublicKey {
    @JsonProperty("bytes")
    public final byte[] key;

    public PublicKey(@JsonProperty("key") byte[] key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicKey publicKey = (PublicKey) o;
        return Arrays.equals(key, publicKey.key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
