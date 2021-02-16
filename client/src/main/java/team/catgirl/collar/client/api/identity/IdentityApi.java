package team.catgirl.collar.client.api.identity;

import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import com.stoyanr.evictor.map.ConcurrentHashMapWithTimedEviction;
import com.stoyanr.evictor.map.EvictibleEntry;
import com.stoyanr.evictor.scheduler.RegularTaskEvictionScheduler;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.identity.CreateTrustRequest;
import team.catgirl.collar.protocol.identity.CreateTrustResponse;
import team.catgirl.collar.protocol.identity.GetIdentityRequest;
import team.catgirl.collar.protocol.identity.GetIdentityResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.TokenGenerator;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Semi-public API for identity setup. If you are using this, expect that the interface may break.
 * This API is not for the feint of heart.
 */
public class IdentityApi extends AbstractApi<IdentityListener> {

    private static final Logger LOGGER = Logger.getLogger(IdentityApi.class.getName());

    // TODO: switch this over to a global task execution scheduler when we use this pattern again
    private final RegularTaskEvictionScheduler<Long, CompletableFuture<ClientIdentity>> identifyFuturesScheduler =  new RegularTaskEvictionScheduler<Long, CompletableFuture<ClientIdentity>>(2, TimeUnit.SECONDS) {
        /**
         * Completes the future if we do not get a response from the server
         * @param entry to evict
         */
        @Override
        protected void onScheduleEviction(EvictibleEntry<Long, CompletableFuture<ClientIdentity>> entry) {
            super.onScheduleEviction(entry);
            entry.getValue().complete(null);
        }
    };

    private final ConcurrentMapWithTimedEviction<Long, CompletableFuture<ClientIdentity>> identifyFutures = new ConcurrentHashMapWithTimedEviction<>(identifyFuturesScheduler);

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
        long id = TokenGenerator.longToken();
        identifyFutures.put(id, future);
        sender.accept(new GetIdentityRequest(identity(), id, playerId));
        return future;
    }

    /**
     * Creates a bi-directional trust between the clients identity and a remote client identity
     * @param identity to create bi-directional trust with
     * @return future for when trust relationship has been created
     */
    public CompletableFuture<ClientIdentity> createTrust(ClientIdentity identity) {
        if (identityStore().isTrustedIdentity(identity)) {
            return CompletableFuture.completedFuture(identity);
        } else {
            CreateTrustRequest request = identityStore().createSendPreKeysRequest(identity, TokenGenerator.longToken());
            CompletableFuture<ClientIdentity> future = new CompletableFuture<>();
            LOGGER.log(Level.INFO, "Creating trust future with " + identity + " and id " + request.id);
            identifyFutures.put(request.id, future);
            sender.accept(request);
            return future;
        }
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.DISCONNECTED) {
            identifyFutures.clear();
        }
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof GetIdentityResponse) {
            GetIdentityResponse response = (GetIdentityResponse) resp;
            CompletableFuture<ClientIdentity> future = identifyFutures.get(response.id);
            future.complete(response.found);
            return true;
        } else if (resp instanceof CreateTrustResponse) {
            CreateTrustResponse response = (CreateTrustResponse) resp;
            identityStore().trustIdentity(response.sender, response.preKeyBundle);
            fireListener("onIdentityTrusted", listener -> {
                listener.onIdentityTrusted(collar, this, identityStore(), response.sender);
            });
            CompletableFuture<ClientIdentity> removed = identifyFutures.remove(response.id);
            if (removed == null) {
                LOGGER.log(Level.INFO, "Sending back a CreateTrustRequest to " + response.sender + " and id " + response.id);
                sender.accept(identityStore().createSendPreKeysRequest(response.sender, response.id));
            } else {
                LOGGER.log(Level.INFO, "Finished creating trust with " + response.sender + " and id " + response.id);
                removed.complete(response.sender);
            }
            return true;
        }
        return false;
    }
}
