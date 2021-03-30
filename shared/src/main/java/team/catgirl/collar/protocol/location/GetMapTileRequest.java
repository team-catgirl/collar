package team.catgirl.collar.protocol.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.location.Dimension;
import team.catgirl.collar.api.location.Layer;
import team.catgirl.collar.api.location.Point;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.ClientIdentity;

public final class GetMapTileRequest extends ProtocolRequest {
    @JsonProperty("point")
    public final Point point;
    @JsonProperty("dimension")
    public final Dimension dimension;
    @JsonProperty("layer")
    public final Layer layer;

    public GetMapTileRequest(@JsonProperty("identity") ClientIdentity identity,
                             @JsonProperty("point") Point point,
                             @JsonProperty("dimension") Dimension dimension,
                             @JsonProperty("layer") Layer layer) {
        super(identity);
        this.point = point;
        this.dimension = dimension;
        this.layer = layer;
    }
}
