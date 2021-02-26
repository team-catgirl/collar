package team.catgirl.collar.sdht.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.sdht.Record;
import team.catgirl.collar.security.ClientIdentity;

public final class DeleteRecordEvent extends AbstractSDHTEvent {
    @JsonProperty("delete")
    public final Record delete;

    public DeleteRecordEvent(@JsonProperty("sender") ClientIdentity sender,
                             @JsonProperty("delete") Record delete) {
        super(sender);
        this.delete = delete;
    }
}
