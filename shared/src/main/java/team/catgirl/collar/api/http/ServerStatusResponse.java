package team.catgirl.collar.api.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerStatusResponse {
    @JsonProperty("status")
    public final String status;

    public ServerStatusResponse(@JsonProperty("status") String status) {
        this.status = status;
    }
}
