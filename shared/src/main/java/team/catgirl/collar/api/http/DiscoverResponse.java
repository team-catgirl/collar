package team.catgirl.collar.api.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class DiscoverResponse {
    @JsonProperty("versions")
    public final List<CollarVersion> versions;

    public DiscoverResponse(@JsonProperty("versions") List<CollarVersion> versions) {
        this.versions = versions;
    }
}
