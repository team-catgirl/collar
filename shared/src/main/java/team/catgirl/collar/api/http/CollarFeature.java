package team.catgirl.collar.api.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class CollarFeature {
    @JsonProperty("name")
    public final String name;
    @JsonProperty("value")
    public final Object value;

    public CollarFeature(@JsonProperty("name") String name, @JsonProperty("value") Object value) {
        this.name = name;
        this.value = value;
    }
}
