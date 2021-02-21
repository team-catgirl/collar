package team.catgirl.collar.client.api.entities;

import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.minecraft.Ticks;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.entities.UpdateEntitiesRequest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EntitiesApi extends AbstractApi<EntitiesListener> {

    private final EntitiesUpdater entitiesUpdater;

    public EntitiesApi(Collar collar,
                       Supplier<ClientIdentityStore> identityStoreSupplier,
                       Consumer<ProtocolRequest> sender,
                       Supplier<List<Entity>> entitiesSupplier,
                       Ticks ticks) {
        super(collar, identityStoreSupplier, sender);
        this.entitiesUpdater = new EntitiesUpdater(entitiesSupplier, this);
        ticks.subscribe(this.entitiesUpdater);
    }

    void updateEntities(List<Entity> entities) {
        sender.accept(new UpdateEntitiesRequest(identity(), entities));
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.CONNECTED) {
            entitiesUpdater.start();
        } else {
            entitiesUpdater.stop();
        }
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        return false;
    }
}
