package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public interface Identity {
    @JsonIgnore
    UUID id();
}
