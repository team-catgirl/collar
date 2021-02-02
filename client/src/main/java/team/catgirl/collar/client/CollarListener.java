package team.catgirl.collar.client;

import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.security.mojang.MinecraftSession;

public interface CollarListener {
    /**
     * Fired on new installation, to confirm that the device is trusted with an authorized collar user
     * @param collar client
     * @param approvalUrl for the user to follow and approve installation on the collar server website
     */
    default void onConfirmDeviceRegistration(Collar collar, String approvalUrl) {}

    /**
     * Fired when the state of the client changes
     * @param collar client
     * @param state of the client connection
     */
    default void onStateChanged(Collar collar, Collar.State state) {}

    /**
     * Fired when the server or client cannot negotiate trust with the client
     * @param collar client
     * @param store identity store to be reset
     */
    default void onClientUntrusted(Collar collar, ClientIdentityStore store) {};

    /**
     * Fired when the server could not validate the minecraft session
     * @param collar client
     * @param session of minecraft client
     */
    default void onMinecraftAccountVerificationFailed(Collar collar, MinecraftSession session) {};
}
