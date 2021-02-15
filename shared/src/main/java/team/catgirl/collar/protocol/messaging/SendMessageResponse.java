package team.catgirl.collar.protocol.messaging;

import team.catgirl.collar.api.messaging.Message;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public class SendMessageResponse extends ProtocolResponse {
    public final ClientIdentity sender;
    public final MinecraftPlayer player;
    public final byte[] message;

    public SendMessageResponse(ServerIdentity identity, ClientIdentity sender, MinecraftPlayer player, byte[] message) {
        super(identity);
        this.sender = sender;
        this.player = player;
        this.message = message;
    }
}
