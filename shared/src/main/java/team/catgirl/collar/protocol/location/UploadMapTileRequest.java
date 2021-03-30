package team.catgirl.collar.protocol.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.MapTile;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class UploadMapTileRequest extends ProtocolRequest {
    @JsonProperty("tile")
    public final MapTile tile;

    public UploadMapTileRequest(@JsonProperty("identity") ClientIdentity identity,
                                @JsonProperty("tile") MapTile tile) {
        super(identity);
        this.tile = tile;
    }
}
