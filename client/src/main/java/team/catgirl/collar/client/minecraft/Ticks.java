package team.catgirl.collar.client.minecraft;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows Collar internals to subscribe to Minecraft client ticks
 */
public final class Ticks {

    private static final Logger LOGGER = Logger.getLogger(Ticks.class.getName());

    private final Set<TickListener> listeners = new HashSet<>();

    public Ticks() {}

    /**
     * @param listener to subscribe
     */
    public void subscribe(TickListener listener) {
        listeners.add(listener);
    }

    /**
     * @param listener to unsubscribe
     */
    public void unsubscribe(TickListener listener) {
        listeners.remove(listener);
    }

    /**
     * Called by the Minecraft client on a tick event
     */
    public void onTick() {
        listeners.forEach(onTick -> {
            try {
                onTick.onTick();
            } catch (Throwable e) {
                LOGGER.log(Level.INFO, "Tick listener failed", e);
            }
        });
    }

    /**
     * @param listener to test
     * @return subscribed
     */
    public boolean isSubscribed(TickListener listener) {
        return listeners.contains(listener);
    }

    public interface TickListener {
        /**
         * Fired on Minecraft client tick
         */
        void onTick();
    }
}
