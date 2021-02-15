package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.KeyPair.PublicKey;

import java.util.UUID;

public interface Identity {
    @JsonIgnore
    UUID id();

    @JsonIgnore
    Integer deviceId();

    @JsonIgnore
    PublicKey publicKey();
}
