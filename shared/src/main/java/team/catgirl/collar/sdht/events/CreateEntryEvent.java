package team.catgirl.collar.sdht.events;

import team.catgirl.collar.sdht.Content;
import team.catgirl.collar.sdht.Record;
import team.catgirl.collar.security.ClientIdentity;

public final class CreateEntryEvent extends AbstractSDHTEvent {
    public final Record record;
    public final Content content;

    public CreateEntryEvent(ClientIdentity sender, Record record, Content content) {
        super(sender);
        this.record = record;
        this.content = content;
    }
}
