package team.catgirl.collar.sdht.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;

/**
 * Sent to all nodes by a joining node
 */
public final class SyncRecordsEvent extends AbstractSDHTEvent {
    @JsonProperty("namespace")
    public final UUID namespace;

    public SyncRecordsEvent(@JsonProperty("sender") ClientIdentity sender,
                            @JsonProperty("namespace") UUID namespace) {
        super(sender);
        this.namespace = namespace;
    }
}
