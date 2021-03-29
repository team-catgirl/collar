package team.catgirl.collar.server.services.tiles;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.api.location.Dimension;
import team.catgirl.collar.api.location.Point;

import java.util.UUID;

public final class MapService {

    private final MongoCollection<Document> docs;

    public MapService(MongoDatabase db) {
        this.docs = db.getCollection("map_tiles");
    }

    public GetMapTileResponse getMapTile(RequestContext context, GetMapTileRequest req) {
        return null;
    }

    public UpsertMapTileResponse upsertMapTile(RequestContext context, UpsertMapTileRequest req) {
        return null;
    }

    public static class GetMapTileRequest {
        public final UUID profile;
        public final Point point;
        public final Dimension dimension;

        public GetMapTileRequest(UUID profile,
                                 Point point,
                                 Dimension dimension) {
            this.profile = profile;
            this.point = point;
            this.dimension = dimension;
        }
    }

    public static class GetMapTileResponse {
        public final byte[] image;

        public GetMapTileResponse(byte[] image) {
            this.image = image;
        }
    }

    public static class UpsertMapTileRequest {
        public final UUID profile;
        public final Point point;
        public final Dimension dimension;

        public UpsertMapTileRequest(UUID profile,
                                 Point point,
                                 Dimension dimension) {
            this.profile = profile;
            this.point = point;
            this.dimension = dimension;
        }
    }

    public static class UpsertMapTileResponse {}
}
