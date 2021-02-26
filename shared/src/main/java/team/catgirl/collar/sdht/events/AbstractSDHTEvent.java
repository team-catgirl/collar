package team.catgirl.collar.sdht.events;

import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Identity;

public class AbstractSDHTEvent {
    public final ClientIdentity sender;

    public AbstractSDHTEvent(ClientIdentity sender) {
        this.sender = sender;
    }
}
