package team.catgirl.collar.protocol.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

/**
 * Sent to any client who is listening for the players location
 * For example, members of groups.
 * It is never sent to the sender
 */
public final class LocationUpdatedResponse extends ProtocolResponse {

    public final ClientIdentity sender;
    public final MinecraftPlayer player;
    public final Location location;

    public LocationUpdatedResponse(@JsonProperty("identity") ServerIdentity identity,
                                   @JsonProperty("sender") ClientIdentity sender,
                                   @JsonProperty("player") MinecraftPlayer player,
                                   @JsonProperty("location") Location location) {
        super(identity);
        this.sender = sender;
        this.player = player;
        this.location = location;
    }
}
