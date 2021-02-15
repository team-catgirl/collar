package team.catgirl.collar.client.api.identity;

import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.identity.GetIdentityRequest;
import team.catgirl.collar.protocol.identity.GetIdentityResponse;
import team.catgirl.collar.protocol.signal.ExchangePreKeysResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Not for the feint of heart.
 */
public class IdentityApi extends AbstractApi<IdentityListener> {

    private final AtomicLong requestIds = new AtomicLong(0L);
    private final ConcurrentMap<Long, CompletableFuture<ClientIdentity>> futures = new ConcurrentHashMap<>();

    public IdentityApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * Ask the server to identify the player ID
     * @param playerId to identify
     * @return identity future
     */
    public CompletableFuture<ClientIdentity> identify(UUID playerId) {
        CompletableFuture<ClientIdentity> future = new CompletableFuture<>();
        long id = requestIds.getAndIncrement();
        futures.put(id, future);
        sender.accept(new GetIdentityRequest(identity(), id, playerId));
        return future;
    }

    /**
     * Creates a bi-directional trust between the clients identity and a remote client identity
     * @param identity to create bi-directional trust with
     * @return future for when trust relationship has been created
     */
    public CompletableFuture<TrustResult> createTrust(ClientIdentity identity) {
        SendPreKeysRequest request = identityStore().createSendPreKeysRequest(identity, requestIds.getAndIncrement());
        CompletableFuture<TrustResult> future = new CompletableFuture<>();
        sender.accept(request);
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
            CompletableFuture<ClientIdentity> future = futures.get(response.id);
            future.complete(response.found);
            return true;
        } else if (resp instanceof ExchangePreKeysResponse) {
            ExchangePreKeysResponse response = (ExchangePreKeysResponse) resp;
            identityStore().trustIdentity(response.owner, response.preKeyBundle);
            sender.accept(identityStore().createSendPreKeysRequest(response.owner, requestIds.getAndIncrement()));
        }
        return false;
    }

    public static final class TrustResult {
        public final ClientIdentity identity;
        public final Boolean trusted;

        public TrustResult(ClientIdentity identity, Boolean trusted) {
            this.identity = identity;
            this.trusted = trusted;
        }
    }
}
