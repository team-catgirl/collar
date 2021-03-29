package team.catgirl.collar.server.services.tiles;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.Binary;
import team.catgirl.collar.api.http.HttpException.ForbiddenException;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.api.location.Dimension;
import team.catgirl.collar.api.location.Point;
import team.catgirl.collar.api.profiles.Role;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public final class MapService {

    public static final String FIELD_OWER = "owner";
    public static final String FIELD_DIMENSION = "dimension";
    public static final String FIELD_COORDINATES = "location.coordinates";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_LOCATION_TYPE = "type";
    public static final String FIELD_LOCATION_COORDINATES = "coordinates";
    public static final String FIELD_IMAGE = "image";
    private final MongoCollection<Document> docs;

    public MapService(MongoDatabase db) {
        this.docs = db.getCollection("map_tiles");
    }

    public GetMapTileResponse getMapTile(RequestContext context, GetMapTileRequest req) {
        if (!context.callerIs(req.profile) || !context.hasRole(Role.ADMINISTRATOR)) {
            throw new ForbiddenException("caller is not same as request");
        }
        Document first = docs.find(and(
                eq(FIELD_OWER, req.profile),
                eq(FIELD_DIMENSION, req.dimension.name()),
                eq(FIELD_COORDINATES, List.of(req.point.x, req.point.y)))).first();
        if (first == null) {
            throw new NotFoundException("not found");
        }
        return new GetMapTileResponse(first.get(FIELD_IMAGE, Binary.class).getData());
    }

    public UpsertMapTileResponse upsertMapTile(RequestContext context, UpsertMapTileRequest req) {
        if (!context.callerIs(req.profile) || !context.hasRole(Role.ADMINISTRATOR)) {
            throw new ForbiddenException("caller is not same as request");
        }
        Map<String, Object> location = Map.of(
                FIELD_LOCATION_TYPE, "Point",
                FIELD_LOCATION_COORDINATES, List.of(req.point.x, req.point.y)
        );
        Map<String, Object> tile = Map.of(
                FIELD_OWER, req.profile,
                FIELD_DIMENSION, req.dimension.name(),
                FIELD_LOCATION, new Document(location),
                FIELD_IMAGE, new Binary(req.image)
        );
        UpdateResult result = docs.updateOne(and(
                eq(FIELD_OWER, req.profile),
                eq(FIELD_DIMENSION, req.dimension.name()),
                eq(FIELD_COORDINATES, List.of(req.point.x, req.point.y))), new Document(tile), new UpdateOptions().upsert(true)
        );
        if (!result.wasAcknowledged() || result.getModifiedCount() <= 0) {
            throw new ServerErrorException("map tile was not upserted");
        }
        return new UpsertMapTileResponse();
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
        public final byte[] image;

        public UpsertMapTileRequest(UUID profile,
                                    Point point,
                                    Dimension dimension,
                                    byte[] image) {
            this.profile = profile;
            this.point = point;
            this.dimension = dimension;
            this.image = image;
        }
    }

    public static class UpsertMapTileResponse {}
}
