package team.catgirl.collar.client.minecraft;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Ticks {

    private static final Logger LOGGER = Logger.getLogger(Ticks.class.getName());

    private final Set<TickListener> listeners = new HashSet<>();

    public Ticks() {}

    public void subscribe(TickListener onTick) {
        listeners.add(onTick);
    }

    public void unsubscribe(TickListener onTick) {
        listeners.remove(onTick);
    }

    public void onTick() {
        listeners.forEach(onTick -> {
            try {
                onTick.onTick();
            } catch (Throwable e) {
                LOGGER.log(Level.INFO, "Tick listener failed", e);
            }
        });
    }

    public boolean isSubscribed(TickListener listener) {
        return listeners.contains(listener);
    }

    public interface TickListener {
        void onTick();
    }
}
