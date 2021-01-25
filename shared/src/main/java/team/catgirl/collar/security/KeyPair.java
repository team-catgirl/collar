package team.catgirl.collar.security;

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
}
