package team.catgirl.collar.protocol.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public final class SendMessageResponse extends ProtocolResponse {
    public final ClientIdentity sender;
    public final MinecraftPlayer player;
    public final byte[] message;

    public SendMessageResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("sender") ClientIdentity sender,
                               @JsonProperty("player") MinecraftPlayer player,
                               @JsonProperty("message") byte[] message) {
        super(identity);
        this.sender = sender;
        this.player = player;
        this.message = message;
    }
}
