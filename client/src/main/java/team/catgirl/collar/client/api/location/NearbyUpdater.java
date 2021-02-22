package team.catgirl.collar.client.api.location;

import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.client.minecraft.Ticks;
import team.catgirl.collar.client.minecraft.Ticks.TickListener;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Updates nearby player states without sending coordinates to the server
 */
public final class NearbyUpdater implements TickListener {

    private final Supplier<Set<Entity>> entitySuppliers;
    private final LocationApi locationApi;
    private final Set<Entity> entities = new HashSet<>();
    private volatile boolean update = false;

    public NearbyUpdater(Supplier<Set<Entity>> entitySuppliers, LocationApi locationApi, Ticks ticks) {
        this.entitySuppliers = entitySuppliers;
        this.locationApi = locationApi;
        ticks.subscribe(this);
    }

    @Override
    public void onTick() {
        if (!update) {
            return;
        }
        Set<Entity> entities = entitySuppliers.get();
        if (!this.entities.equals(entities)) {
            locationApi.publishNearby(entities);
            this.entities.clear();
            this.entities.addAll(entities);
        }
    }

    public void start() {
        entities.clear();
        update = true;
    }

    public void stop() {
        entities.clear();
        update = false;
    }
}
