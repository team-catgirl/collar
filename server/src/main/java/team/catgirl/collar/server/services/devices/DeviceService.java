package team.catgirl.collar.server.services.devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import team.catgirl.collar.api.http.HttpException.BadRequestException;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.UnauthorisedException;
import team.catgirl.collar.server.http.RequestContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public final class DeviceService {

    private static final int MAX_DEVICES = 100;
    private static final String FIELD_OWNER = "owner";
    private static final String FIELD_DEVICE_ID = "deviceId";
    private static final String FIELD_PUBLIC_KEY = "publicKey";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_FINGERPRINT = "fingerprint";

    private final MongoCollection<Document> docs;

    public DeviceService(MongoDatabase db) {
        this.docs = db.getCollection("devices");
        Map<String, Object> index = new HashMap<>();
        index.put(FIELD_OWNER, 1);
        index.put(FIELD_DEVICE_ID, 1);
        this.docs.createIndex(new Document(index));
    }

    public CreateDeviceResponse createDevice(RequestContext context, CreateDeviceRequest req) {
        if (req.name == null) {
            throw new BadRequestException("name missing");
        }
        if (req.owner == null) {
            throw new BadRequestException("owner missing");
        }
        if (!context.owner.equals(req.owner)) {
            throw new UnauthorisedException("not owner");
        }
        int newDeviceId = findDevices(context, FindDevicesRequest.byOwner(req.owner)).devices.stream()
                .mapToInt(value -> value.deviceId)
                .max()
                .orElse(0) + 1;

        // Do not allow the creation of more devices
        if (newDeviceId >= MAX_DEVICES) {
            throw new BadRequestException("Too many devices registered. Please delete some.");
        }

        Map<String, Object> state = new HashMap<>();
        state.put(FIELD_OWNER, req.owner);
        state.put(FIELD_DEVICE_ID, newDeviceId);
        state.put(FIELD_NAME, req.name);
        InsertOneResult result = docs.insertOne(new Document(state));
        ObjectId value = Objects.requireNonNull(result.getInsertedId()).asObjectId().getValue();
        Device device = docs.find(eq("_id", value)).map(DeviceService::map).first();
        if (device == null) {
            throw new NotFoundException("cannot find created device");
        }
        return new CreateDeviceResponse(device);
    }

    public FindDevicesResponse findDevices(RequestContext context, FindDevicesRequest req) {
        FindIterable<Document> cursor;
        if (req.byOwner != null && req.byOwner.equals(context.owner)) {
            cursor = docs.find(eq(FIELD_OWNER, req.byOwner));
        } else {
            throw new UnauthorisedException("not owner");
        }
        return new FindDevicesResponse(StreamSupport.stream(cursor.spliterator(), false).map(DeviceService::map).collect(Collectors.toList()));
    }

    public DeleteDeviceResponse deleteDevice(RequestContext context, DeleteDeviceRequest req) {
        if (!context.owner.equals(req.owner)) {
            throw new UnauthorisedException("not owner");
        }
        if (docs.deleteOne(and(eq(FIELD_OWNER, req.owner), eq(FIELD_DEVICE_ID, req.deviceId))).getDeletedCount() != 1) {
            throw new NotFoundException("could not find device");
        }
        return new DeleteDeviceResponse();
    }

    private static Device map(Document document) {
        UUID player = document.get(FIELD_OWNER, UUID.class);
        int deviceId = document.getInteger(FIELD_DEVICE_ID);
        String deviceName = document.getString(FIELD_NAME);
        return new Device(player, deviceId, deviceName);
    }

    public static class CreateDeviceRequest {
        @JsonProperty(FIELD_OWNER)
        public final UUID owner;
        @JsonProperty(FIELD_NAME)
        public final String name;

        public CreateDeviceRequest(
                @JsonProperty(FIELD_OWNER) UUID owner,
                @JsonProperty(FIELD_NAME) String name
        ) {
            this.owner = owner;
            this.name = name;
        }
    }

    public static class CreateDeviceResponse {
        @JsonProperty("verificationUrl")
        public final Device device;

        public CreateDeviceResponse(@JsonProperty("device") Device device) {
            this.device = device;
        }
    }

    public static class FindDevicesRequest {
        @JsonProperty("byOwner")
        public final UUID byOwner;

        public FindDevicesRequest(
                @JsonProperty("byOwner") UUID byOwner)
        {
            this.byOwner = byOwner;
        }

        public static FindDevicesRequest byOwner(UUID owner) {
            return new FindDevicesRequest(owner);
        }
    }

    public static class FindDevicesResponse {
        @JsonProperty("devices")
        public final List<Device> devices;

        public FindDevicesResponse(@JsonProperty("devices") List<Device> devices) {
            this.devices = devices;
        }
    }

    public static class DeleteDeviceRequest {
        @JsonProperty(FIELD_OWNER)
        public final UUID owner;
        @JsonProperty(FIELD_DEVICE_ID)
        public final Integer deviceId;

        public DeleteDeviceRequest(@JsonProperty(FIELD_OWNER) UUID owner, @JsonProperty(FIELD_DEVICE_ID) Integer deviceId) {
            this.owner = owner;
            this.deviceId = deviceId;
        }
    }

    public static class DeleteDeviceResponse {}
}
