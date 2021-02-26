package team.catgirl.collar.sdht.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import team.catgirl.collar.security.ClientIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "t")
public abstract class AbstractSDHTEvent {
    @JsonProperty("sender")
    public final ClientIdentity sender;

    public AbstractSDHTEvent(@JsonProperty("sender") ClientIdentity sender) {
        this.sender = sender;
    }
}
