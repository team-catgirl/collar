package team.catgirl.collar.security.keys;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        public final byte[] fingerPrint;
        @JsonProperty("bytes")
        public final byte[] bytes;

        public PublicKey(@JsonProperty("fingerPrint") byte[] fingerPrint, @JsonProperty("bytes") byte[] bytes) {
            this.fingerPrint = fingerPrint;
            this.bytes = bytes;
        }
    }

    public static final class PrivateKey {
        @JsonProperty("bytes")
        public final byte[] bytes;

        public PrivateKey(@JsonProperty("bytes") byte[] bytes) {
            this.bytes = bytes;
        }
    }
}
