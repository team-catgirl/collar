package team.catgirl.collar.client;

import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;

public interface CollarListener {
    /**
     * Fired on new installation, to confirm that the device is trusted with an authorized collar user
     * @param collar client
     * @param resp response
     */
    default void onConfirmDeviceRegistration(Collar collar, RegisterDeviceResponse resp) {}

    /**
     * Fired when the state of the client changes
     * @param collar client
     * @param state of the client connection
     */
    default void onStateChanged(Collar collar, State state) {}

    /**
     * Fired when the server or client cannot negotiate trust with the client
     * @param collar
     * @param store
     */
    default void onClientUntrusted(Collar collar, ClientIdentityStore store) {};
}
