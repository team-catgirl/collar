package team.catgirl.collar.sdht.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.sdht.Record;
import team.catgirl.collar.security.ClientIdentity;

public final class SyncContentEvent extends AbstractSDHTEvent {
    @JsonProperty("recipient")
    public final ClientIdentity recipient;
    @JsonProperty("record")
    public final Record record;

    public SyncContentEvent(@JsonProperty("sender") ClientIdentity sender,
                            @JsonProperty("recipient") ClientIdentity recipient,
                            @JsonProperty("record") Record record) {
        super(sender);
        this.recipient = recipient;
        this.record = record;
    }
}
