package team.catgirl.collar.protocol.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.UUID;

public final class SendMessageResponse extends ProtocolResponse {
    @JsonProperty("individual")
    public final ClientIdentity individual;
    @JsonProperty("group")
    public final UUID group;
    @JsonProperty("player")
    public final MinecraftPlayer player;
    @JsonProperty("message")
    public final byte[] message;

    public SendMessageResponse(@JsonProperty("identity") ServerIdentity identity,
                               @JsonProperty("individual") ClientIdentity individual,
                               @JsonProperty("group") UUID group,
                               @JsonProperty("player") MinecraftPlayer player,
                               @JsonProperty("message") byte[] message) {
        super(identity);
        this.individual = individual;
        this.group = group;
        this.player = player;
        this.message = message;
    }
}
