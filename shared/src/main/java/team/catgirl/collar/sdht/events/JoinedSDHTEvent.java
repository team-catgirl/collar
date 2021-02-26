package team.catgirl.collar.sdht.events;

import team.catgirl.collar.security.ClientIdentity;

public final class JoinedSDHTEvent extends AbstractSDHTEvent {
    public JoinedSDHTEvent(ClientIdentity sender) {
        super(sender);
    }
}
