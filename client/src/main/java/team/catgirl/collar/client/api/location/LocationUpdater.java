package team.catgirl.collar.client.api.location;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocationUpdater {
    private final LocationApi locationApi;
    private ScheduledExecutorService scheduler;

    public LocationUpdater(LocationApi locationApi) {
        this.locationApi = locationApi;
    }

    public boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }

    public void start() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(locationApi::publishLocation, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
