package team.catgirl.collar.server.services.devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.types.Binary;
import team.catgirl.collar.security.KeyPair.PublicKey;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.server.http.HttpException;
import team.catgirl.collar.server.http.HttpException.BadRequestException;
import team.catgirl.collar.server.http.HttpException.NotFoundException;
import team.catgirl.collar.server.http.HttpException.UnauthorisedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public final class DeviceService {

    public static final int MAX_DEVICES = 100;
    private final MongoCollection<Document> docs;

    public DeviceService(MongoDatabase db) {
        this.docs = db.getCollection("devices");
        Map<String, Object> index = new HashMap<>();
        index.put("owner", 1);
        index.put("deviceId", 1);
        this.docs.createIndex(new Document(index));
    }

    public CreateDeviceResponse createDevice(PlayerIdentity identity, CreateDeviceRequest req) {
        if (req.name == null) {
            throw new BadRequestException("name");
        }
        if (req.owner == null) {
            throw new BadRequestException("owner");
        }
        if (req.publicKey == null) {
            throw new BadRequestException("publicKey");
        }
        if (!identity.player.equals(req.owner)) {
            throw new UnauthorisedException("unauthorized");
        }
        int newDeviceId = findDevices(FindDevicesRequest.byOwner(req.owner)).devices.stream()
                .mapToInt(value -> value.deviceId)
                .max()
                .orElse(1);

        // Do not allow the creation of more devices
        if (newDeviceId >= MAX_DEVICES) {
            throw new BadRequestException("Too many devices registered. Please delete some.");
        }

        Map<String, Object> state = new HashMap<>();
        state.put("owner", req.owner);
        state.put("deviceId", newDeviceId);
        state.put("name", req.name);
        state.put("publicKey", new Binary(req.publicKey.key));

        InsertOneResult result = docs.insertOne(new Document(state));
        FindDevicesResponse devices = findDevices(FindDevicesRequest.findOne(req.owner, newDeviceId));
        Device device = devices.devices.stream().findFirst().orElseThrow(() -> new HttpException.NotFoundException("could not find created device"));
        return new CreateDeviceResponse(device);
    }

    public FindDevicesResponse findDevices(FindDevicesRequest req) {
        throw new IllegalStateException("not implemented");
    }

    public DeleteDeviceResponse deleteDevice(DeleteDeviceRequest req) {
        if (docs.deleteOne(and(eq("owner", req.owner))).getDeletedCount() != 1) {
            throw new NotFoundException("could not find device");
        }
        return new DeleteDeviceResponse();
    }

    public static class CreateDeviceRequest {
        @JsonProperty("owner")
        public final UUID owner;
        @JsonProperty("name")
        public final String name;
        @JsonProperty("publicKey")
        public final PublicKey publicKey;

        public CreateDeviceRequest(
                @JsonProperty("owner") UUID owner,
                @JsonProperty("name") String name,
                @JsonProperty("publicKey") PublicKey publicKey
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
        @JsonProperty("byDeviceId")
        public final Integer byDeviceId;

        public FindDevicesRequest(
                @JsonProperty("byOwner") UUID byOwner,
                @JsonProperty("byDeviceId") Integer byDeviceId)
        {
            this.byOwner = byOwner;
            this.byDeviceId = byDeviceId;
        }

        public static FindDevicesRequest findOne(UUID owner, int deviceId) {
            return new FindDevicesRequest(owner, deviceId);
        }

        public static FindDevicesRequest byOwner(UUID owner) {
            return new FindDevicesRequest(owner, null);
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
        @JsonProperty("owner")
        public final UUID owner;
        @JsonProperty("deviceId")
        public final Integer deviceId;

        public DeleteDeviceRequest(@JsonProperty("owner") UUID owner, @JsonProperty("deviceId") Integer deviceId) {
            this.owner = owner;
            this.deviceId = deviceId;
        }
    }

    public static class DeleteDeviceResponse {}
}
