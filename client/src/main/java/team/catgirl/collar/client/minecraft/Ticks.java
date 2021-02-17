package team.catgirl.collar.client.minecraft;

import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.client.api.location.LocationUpdater;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Ticks {

    private static final Logger LOGGER = Logger.getLogger(Ticks.class.getName());

    private final Set<TickListener> listeners = new HashSet<>();
    public final Ticker ticker;

    public Ticks() {
        this.ticker = new Ticker();
    }

    public void subscribe(TickListener onTick) {
        listeners.add(onTick);
    }

    public void unsubscribe(TickListener onTick) {
        listeners.remove(onTick);
    }

    private void fireOnTick() {
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

    public final class Ticker {
        public void onTick() {
            fireOnTick();
        }
    }
}
