package team.catgirl.collar.security;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Identity {
    @JsonIgnore
    String getName();
}
