package team.catgirl.collar.client.api.location;

import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.client.minecraft.Ticks;
import team.catgirl.collar.client.minecraft.Ticks.TickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NearbyUpdater implements TickListener {

    private final Supplier<List<Entity>> entitySuppliers;
    private final LocationApi locationApi;
    private final List<Entity> entities = new ArrayList<>();
    private volatile boolean update = false;

    public NearbyUpdater(Supplier<List<Entity>> entitySuppliers, LocationApi locationApi, Ticks ticks) {
        this.entitySuppliers = entitySuppliers;
        this.locationApi = locationApi;
        ticks.subscribe(this);
    }

    @Override
    public void onTick() {
        if (!update) {
            return;
        }
        List<Entity> entities = entitySuppliers.get();
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
