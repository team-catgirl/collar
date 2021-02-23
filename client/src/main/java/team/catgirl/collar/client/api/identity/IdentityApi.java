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
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.security.TokenGenerator;

import java.util.Optional;
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
    private final RegularTaskEvictionScheduler<Long, CompletableFuture<Optional<ClientIdentity>>> identifyFuturesScheduler = new RegularTaskEvictionScheduler<Long, CompletableFuture<Optional<ClientIdentity>>>(1, TimeUnit.SECONDS) {
        /**
         * Completes the future if we do not get a response from the server
         * @param entry to evict
         */
        @Override
        protected void onScheduleEviction(EvictibleEntry<Long, CompletableFuture<Optional<ClientIdentity>>> entry) {
            super.onScheduleEviction(entry);
            entry.getValue().complete(null);
        }
    };

    private final ConcurrentMapWithTimedEviction<Long, CompletableFuture<Optional<ClientIdentity>>> identifyFutures = new ConcurrentHashMapWithTimedEviction<>(identifyFuturesScheduler);

    public IdentityApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * Ask the server to identify the player ID
     * @param playerId to identify
     * @return identity future
     */
    public CompletableFuture<Optional<ClientIdentity>> identify(UUID playerId) {
        CompletableFuture<Optional<ClientIdentity>> future = new CompletableFuture<>();
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
    public CompletableFuture<Optional<ClientIdentity>> createTrust(Optional<ClientIdentity> identity) {
        ClientIdentity clientIdentity = identity.orElse(null);
        if (clientIdentity == null || identityStore().isTrustedIdentity(clientIdentity)) {
            return CompletableFuture.completedFuture(identity);
        } else {
            CreateTrustRequest request = identityStore().createSendPreKeysRequest(clientIdentity, TokenGenerator.longToken());
            CompletableFuture<Optional<ClientIdentity>> future = new CompletableFuture<>();
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
            CompletableFuture<Optional<ClientIdentity>> future = identifyFutures.get(response.id);
            future.complete(response.found == null ? Optional.empty() : Optional.of(response.found));
            return true;
        } else if (resp instanceof CreateTrustResponse) {
            CreateTrustResponse response = (CreateTrustResponse) resp;
            identityStore().trustIdentity(response.sender, response.preKeyBundle);
            fireListener("onIdentityTrusted", listener -> {
                listener.onIdentityTrusted(collar, this, identityStore(), response.sender);
            });
            CompletableFuture<Optional<ClientIdentity>> removed = identifyFutures.remove(response.id);
            if (removed == null) {
                LOGGER.log(Level.INFO, "Sending back a CreateTrustRequest to " + response.sender + " and id " + response.id);
                sender.accept(identityStore().createSendPreKeysRequest(response.sender, response.id));
            } else {
                LOGGER.log(Level.INFO, "Finished creating trust with " + response.sender + " and id " + response.id);
                removed.complete(response.sender == null ? Optional.empty() : Optional.of(response.sender));
            }
            return true;
        }
        return false;
    }
}
