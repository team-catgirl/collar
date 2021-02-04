package team.catgirl.collar.api.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class DiscoverResponse {
    @JsonProperty("versions")
    public final List<CollarVersion> versions;
    @JsonProperty("features")
    public final List<CollarFeature> features;

    public DiscoverResponse(@JsonProperty("versions") List<CollarVersion> versions, @JsonProperty("features") List<CollarFeature> features) {
        this.versions = versions;
        this.features = features;
    }
}
