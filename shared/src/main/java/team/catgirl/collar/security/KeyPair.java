package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

public final class KeyPair {
    @JsonProperty("publicKey")
    public final PublicKey publicKey;
    @JsonProperty("privateKey")
    public final PrivateKey privateKey;

    public KeyPair(
            @JsonProperty("publicKey") PublicKey publicKey,
            @JsonProperty("privateKey") PrivateKey privateKey
    ) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public static final class PublicKey {
        @JsonProperty("fingerPrint")
        public final String fingerPrint;
        @JsonProperty("bytes")
        public final byte[] key;

        public PublicKey(@JsonProperty("fingerPrint") String fingerPrint, @JsonProperty("key") byte[] key) {
            this.fingerPrint = fingerPrint;
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PublicKey publicKey = (PublicKey) o;
            return fingerPrint.equals(publicKey.fingerPrint) &&
                    Arrays.equals(key, publicKey.key);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(fingerPrint);
            result = 31 * result + Arrays.hashCode(key);
            return result;
        }
    }

    public static final class PrivateKey {
        @JsonProperty("key")
        public final byte[] key;

        public PrivateKey(@JsonProperty("key") byte[] key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrivateKey that = (PrivateKey) o;
            return Arrays.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(key);
        }
    }
}
