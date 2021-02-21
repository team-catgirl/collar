package team.catgirl.collar.client.api.entities;

import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.client.minecraft.Ticks.TickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class EntitiesUpdater implements TickListener {

    private final Supplier<List<Entity>> entitySuppliers;
    private final EntitiesApi entitiesApi;
    private final List<Entity> entities = new ArrayList<>();
    private volatile boolean update = false;

    public EntitiesUpdater(Supplier<List<Entity>> entitySuppliers, EntitiesApi entitiesApi) {
        this.entitySuppliers = entitySuppliers;
        this.entitiesApi = entitiesApi;
    }

    @Override
    public void onTick() {
        if (!update) {
            return;
        }
        List<Entity> entities = entitySuppliers.get();
        if (!this.entities.equals(entities)) {
            entitiesApi.updateEntities(entities);
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
