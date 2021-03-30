package team.catgirl.collar.protocol.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.MapTile;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;

public final class GetMapTileResponse extends ProtocolResponse {

    @JsonProperty("tile")
    public final MapTile tile;

    public GetMapTileResponse(@JsonProperty("identity") ServerIdentity identity,
                              @JsonProperty("tile") MapTile tile) {
        super(identity);
        this.tile = tile;
    }
}
