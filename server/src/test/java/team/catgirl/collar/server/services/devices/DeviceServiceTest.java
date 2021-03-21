package team.catgirl.collar.server.services.devices;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.api.profiles.Role;
import team.catgirl.collar.server.junit.MongoDatabaseTestRule;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DeviceServiceTest {
    @Rule
    public MongoDatabaseTestRule dbRule = new MongoDatabaseTestRule();
    private DeviceService devices;

    @Before
    public void services() {
        devices = new DeviceService(dbRule.db);
    }

    @Test
    public void createDevice() {
        UUID owner = UUID.randomUUID();
        RequestContext ctx = new RequestContext(owner, Set.of(Role.PLAYER));
        Device device = devices.createDevice(ctx, new DeviceService.CreateDeviceRequest(owner, "Cool Computer")).device;
        Assert.assertEquals(1, device.deviceId);
        Assert.assertEquals("Cool Computer", device.name);
        Assert.assertEquals(owner, device.owner);

        Device device2 = devices.createDevice(ctx, new DeviceService.CreateDeviceRequest(owner, "Lazy Laptop")).device;
        Assert.assertEquals(2, device2.deviceId);
        Assert.assertEquals("Lazy Laptop", device2.name);
        Assert.assertEquals(owner, device2.owner);
    }

    @Test
    public void findDevice() {
        RequestContext owner1 = new RequestContext(UUID.randomUUID(), Set.of(Role.PLAYER));
        RequestContext owner2 = new RequestContext(UUID.randomUUID(), Set.of(Role.PLAYER));
        devices.createDevice(owner1, new DeviceService.CreateDeviceRequest(owner1.owner, "Cool Computer"));
        devices.createDevice(owner2, new DeviceService.CreateDeviceRequest(owner2.owner, "Lazy Laptop"));

        List<Device> allDevices = this.devices.findDevices(owner1, DeviceService.FindDevicesRequest.byOwner(owner1.owner)).devices;
        Assert.assertEquals(1, allDevices.size());
        Device device = allDevices.get(0);
        Assert.assertEquals("Cool Computer", device.name);
        Assert.assertEquals(1, device.deviceId);
        Assert.assertEquals(owner1.owner, device.owner);
    }

    @Test
    public void deleteDevice() {
        RequestContext owner1 = new RequestContext(UUID.randomUUID(), Set.of(Role.PLAYER));
        RequestContext owner2 = new RequestContext(UUID.randomUUID(), Set.of(Role.PLAYER));
        devices.createDevice(owner1, new DeviceService.CreateDeviceRequest(owner1.owner, "Cool Computer"));
        devices.createDevice(owner2, new DeviceService.CreateDeviceRequest(owner2.owner, "Lazy Laptop"));

        devices.deleteDevice(owner1, new DeviceService.DeleteDeviceRequest(owner1.owner, 1));
        Assert.assertEquals(0, this.devices.findDevices(owner1, DeviceService.FindDevicesRequest.byOwner(owner1.owner)).devices.size());
        Assert.assertEquals(1, this.devices.findDevices(owner2, DeviceService.FindDevicesRequest.byOwner(owner2.owner)).devices.size());
    }
}
