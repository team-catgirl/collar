package team.catgirl.collar.sdht.events;

import team.catgirl.collar.sdht.Record;
import team.catgirl.collar.security.ClientIdentity;

public final class DeleteRecordEvent extends AbstractSDHTEvent {
    public final Record delete;

    public DeleteRecordEvent(ClientIdentity sender, Record delete) {
        super(sender);
        this.delete = delete;
    }
}
