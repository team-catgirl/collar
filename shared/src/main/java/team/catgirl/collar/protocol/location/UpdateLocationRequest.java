package team.catgirl.collar.protocol.location;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

/**
 * Sent by the client to update the players current location
 * There is no response sent back to the sender for this request as this would be too chatty.
 */
public final class UpdateLocationRequest extends ProtocolRequest {
    @JsonProperty("location")
    public final Location location;
    @JsonProperty("player")
    public final MinecraftPlayer player;

    @JsonCreator
    public UpdateLocationRequest(@JsonProperty("identity") ClientIdentity identity,
                                 @JsonProperty("player") MinecraftPlayer player,
                                 @JsonProperty("location") Location location) {
        super(identity);
        this.player = player;
        this.location = location;
    }
}
