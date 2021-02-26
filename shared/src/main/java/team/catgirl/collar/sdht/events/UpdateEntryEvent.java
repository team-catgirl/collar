package team.catgirl.collar.sdht.events;

import team.catgirl.collar.sdht.Content;
import team.catgirl.collar.sdht.Record;
import team.catgirl.collar.security.ClientIdentity;

public final class UpdateEntryEvent extends AbstractSDHTEvent {
    public final Record replace;
    public final Record updated;
    public final Content content;

    public UpdateEntryEvent(ClientIdentity sender, Record replace, Record updated, Content content) {
        super(sender);
        this.replace = replace;
        this.updated = updated;
        this.content = content;
    }
}
