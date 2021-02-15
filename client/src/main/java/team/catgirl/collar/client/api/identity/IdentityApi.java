package team.catgirl.collar.client.api.identity;

import com.google.common.util.concurrent.SettableFuture;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.identity.GetIdentityRequest;
import team.catgirl.collar.protocol.identity.GetIdentityResponse;
import team.catgirl.collar.protocol.signal.ExchangePreKeysResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IdentityApi extends AbstractApi<IdentityListener> {

    private final AtomicLong requestIds = new AtomicLong(0L);
    private final ConcurrentMap<Long, SettableFuture<ClientIdentity>> futures = new ConcurrentHashMap<>();

    public IdentityApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * Ask the server to identify the player ID
     * @param playerId to identify
     * @return identity future
     */
    public Future<ClientIdentity> identify(UUID playerId) {
        SettableFuture<ClientIdentity> future = SettableFuture.create();
        long id = requestIds.getAndIncrement();
        futures.put(id, future);
        sender.accept(new GetIdentityRequest(identity(), id, playerId));
        return future;
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.DISCONNECTED) {
            futures.clear();
        }
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof GetIdentityResponse) {
            GetIdentityResponse response = (GetIdentityResponse) resp;
            SettableFuture<ClientIdentity> future = futures.get(response.id);
            future.set(response.found);
            return true;
        } else if (resp instanceof ExchangePreKeysResponse) {
            ExchangePreKeysResponse response = (ExchangePreKeysResponse) resp;
            identityStore().trustIdentity(response.owner, response.preKeyBundle);
            sender.accept(identityStore().createSendPreKeysRequest(response.owner));
        }
        return false;
    }
}
