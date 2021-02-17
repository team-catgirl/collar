package team.catgirl.collar.client.api.location;

import team.catgirl.collar.client.minecraft.Ticks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocationUpdater implements Ticks.TickListener {
    private final LocationApi locationApi;
    private final Ticks ticks;

    public LocationUpdater(LocationApi locationApi, Ticks ticks) {
        this.locationApi = locationApi;
        this.ticks = ticks;
    }

    public boolean isRunning() {
        return ticks.isSubscribed(this);
    }

    public void start() {
        ticks.subscribe(this);
    }

    public void stop() {
        ticks.unsubscribe(this);
    }

    @Override
    public void onTick() {
        locationApi.publishLocation();
    }
}
