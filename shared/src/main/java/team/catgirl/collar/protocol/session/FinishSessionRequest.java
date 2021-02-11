package team.catgirl.collar.protocol.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public class FinishSessionRequest extends ProtocolRequest {
    @JsonProperty("player")
    public MinecraftPlayer player;

    @JsonCreator
    public FinishSessionRequest(@JsonProperty("identity") ClientIdentity identity, @JsonProperty("player") MinecraftPlayer p) {
        super(identity);
        this.player=p;
    }
}