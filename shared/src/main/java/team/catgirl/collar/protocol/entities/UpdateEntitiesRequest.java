package team.catgirl.collar.protocol.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.List;

public final class UpdateEntitiesRequest extends ProtocolRequest {
    public final List<Entity> entities;

    public UpdateEntitiesRequest(@JsonProperty("identity") ClientIdentity identity,
                                 @JsonProperty("entities") List<Entity> entities) {
        super(identity);
        this.entities = entities;
    }
}
