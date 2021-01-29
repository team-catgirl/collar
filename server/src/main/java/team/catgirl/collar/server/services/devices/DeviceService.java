package team.catgirl.collar.server.services.devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import team.catgirl.collar.security.KeyPair.PublicKey;
import team.catgirl.collar.server.http.HttpException.BadRequestException;
import team.catgirl.collar.server.http.HttpException.NotFoundException;
import team.catgirl.collar.server.http.HttpException.UnauthorisedException;
import team.catgirl.collar.server.http.RequestContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public final class DeviceService {

    public static final int MAX_DEVICES = 100;
    public static final String FIELD_OWNER = "owner";
    public static final String FIELD_DEVICE_ID = "deviceId";
    public static final String FIELD_PUBLIC_KEY = "publicKey";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_FINGERPRINT = "fingerprint";
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
            throw new BadRequestException(FIELD_NAME);
        }
        if (req.owner == null) {
            throw new BadRequestException(FIELD_OWNER);
        }
        if (req.publicKey == null) {
            throw new BadRequestException(FIELD_PUBLIC_KEY);
        }
        if (!context.profileId.equals(req.owner)) {
            throw new UnauthorisedException("not owner");
        }
        int newDeviceId = findDevices(context, FindDevicesRequest.byOwner(req.owner)).devices.stream()
                .mapToInt(value -> value.deviceId)
                .max()
                .orElse(1);

        // Do not allow the creation of more devices
        if (newDeviceId >= MAX_DEVICES) {
            throw new BadRequestException("Too many devices registered. Please delete some.");
        }

        Map<String, Object> state = new HashMap<>();
        state.put(FIELD_OWNER, req.owner);
        state.put(FIELD_DEVICE_ID, newDeviceId);
        state.put(FIELD_NAME, req.name);
        state.put(FIELD_PUBLIC_KEY, new Binary(req.publicKey.key));
        state.put(FIELD_FINGERPRINT, req.publicKey.fingerPrint);

        InsertOneResult result = docs.insertOne(new Document(state));
        ObjectId value = Objects.requireNonNull(result.getInsertedId()).asObjectId().getValue();
        return new CreateDeviceResponse(docs.find(eq("_id", value)).map(DeviceService::map).first());
    }

    public FindDevicesResponse findDevices(RequestContext context, FindDevicesRequest req) {
        if (!context.profileId.equals(req.byOwner)) {
            throw new UnauthorisedException("not owner");
        }
        FindIterable<Document> cursor;
        if (req.byOwner != null) {
            cursor = docs.find(eq(FIELD_OWNER, req.byOwner));
        } else {
            throw new BadRequestException("empty request");
        }
        return new FindDevicesResponse(StreamSupport.stream(cursor.spliterator(), false).map(DeviceService::map).collect(Collectors.toList()));
    }

    public DeleteDeviceResponse deleteDevice(RequestContext context, DeleteDeviceRequest req) {
        if (!context.profileId.equals(req.owner)) {
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
        byte[] publicKeyBytes = document.get(FIELD_PUBLIC_KEY, Binary.class).getData();
        String fingerprint = document.get(FIELD_FINGERPRINT, String.class);
        PublicKey publicKey = new PublicKey(fingerprint, publicKeyBytes);
        return new Device(player, deviceId, deviceName, publicKey);
    }

    public static class CreateDeviceRequest {
        @JsonProperty(FIELD_OWNER)
        public final UUID owner;
        @JsonProperty(FIELD_NAME)
        public final String name;
        @JsonProperty(FIELD_PUBLIC_KEY)
        public final PublicKey publicKey;

        public CreateDeviceRequest(
                @JsonProperty(FIELD_OWNER) UUID owner,
                @JsonProperty(FIELD_NAME) String name,
                @JsonProperty(FIELD_PUBLIC_KEY) PublicKey publicKey
        ) {
            this.owner = owner;
            this.name = name;
            this.publicKey = publicKey;
        }
    }

    public static class CreateDeviceResponse {
        public final Device device;

        public CreateDeviceResponse(Device device) {
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
