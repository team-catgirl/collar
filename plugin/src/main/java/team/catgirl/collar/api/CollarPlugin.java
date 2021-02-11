package team.catgirl.collar.api;

import team.catgirl.collar.client.Collar;

/**
 * Used for Minecraft mod's to hook into the collar core mod
 */
public interface CollarPlugin {
    /**
     * Called when the collar plugin is loaded
     * If collar's core mod is not enabled, this function will not be called.
     */
    default void onLoad() {}

    /**
     * When the client is connecting
     * @param collar client instance
     */
    default void onConnecting(Collar collar) {}

    /**
     * When the client is connected
     * @param collar client instance
     */
    default void onConnected(Collar collar) {}

    /**
     * When the client is disconnected
     * @param collar client instance
     */
    default void onDisconnected(Collar collar) {}
}
