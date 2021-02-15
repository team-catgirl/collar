package team.catgirl.collar.protocol.messaging;

import team.catgirl.collar.api.messaging.Message;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public class SendMessageRequest extends ProtocolRequest {
    public final ClientIdentity recipient;
    public final byte[] message;

    public SendMessageRequest(ClientIdentity identity, ClientIdentity recipient, byte[] message) {
        super(identity);
        this.recipient = recipient;
        this.message = message;
    }
}
