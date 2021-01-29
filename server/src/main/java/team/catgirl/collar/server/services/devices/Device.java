package team.catgirl.collar.server.services.devices;

import team.catgirl.collar.security.KeyPair.PublicKey;

import java.util.UUID;

public final class Device {
    public final UUID player;
    public final int deviceId;
    public final String deviceName;
    public final PublicKey publicKey;

    public Device(UUID player, int deviceId, String deviceName, PublicKey publicKey) {
        this.player = player;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.publicKey = publicKey;
    }
}
