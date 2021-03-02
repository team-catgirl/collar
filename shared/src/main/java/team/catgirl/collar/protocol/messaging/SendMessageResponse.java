package team.catgirl.collar.protocol.messaging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.UUID;

public final class SendMessageResponse extends ProtocolResponse {
    @JsonProperty("sender")
    public final Identity sender;
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("player")
    public final Player player;
    @JsonProperty("message")
    public final byte[] message;

    @JsonCreator
    public SendMessageResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("sender") Identity sender,
                               @JsonProperty("group") UUID group,
                               @JsonProperty("player") Player player,
                               @JsonProperty("message") byte[] message) {
        super(identity);
        this.sender = sender;
        this.group = group;
        this.player = player;
        this.message = message;
    }
}
