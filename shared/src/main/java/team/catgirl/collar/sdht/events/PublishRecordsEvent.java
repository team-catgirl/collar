package team.catgirl.collar.sdht.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.sdht.Record;
import team.catgirl.collar.security.ClientIdentity;

import java.util.Set;

public final class PublishRecordsEvent extends AbstractSDHTEvent {
    @JsonProperty("records")
    public final Set<Record> records;
    @JsonProperty("recipient")
    public final ClientIdentity recipient;

    public PublishRecordsEvent(@JsonProperty("sender") ClientIdentity sender,
                               @JsonProperty("records") Set<Record> records,
                               @JsonProperty("recipient") ClientIdentity recipient) {
        super(sender);
        this.records = records;
        this.recipient = recipient;
    }
}
