package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "t")
public interface Identity {
    @JsonIgnore
    UUID id();

    @JsonIgnore
    Integer deviceId();

    @JsonIgnore
    PublicKey publicKey();
}
