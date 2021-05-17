package team.catgirl.collar.client.api.identity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import com.stoyanr.evictor.map.ConcurrentHashMapWithTimedEviction;
import com.stoyanr.evictor.map.EvictibleEntry;
import com.stoyanr.evictor.scheduler.RegularTaskEvictionScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.identity.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Identity API used for resolving Collar identities from minecraft player IDs
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

    private final Cache<UUID, CompletableFuture<Optional<PublicProfile>>> profileFutures = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .removalListener(removalNotification -> {
        if (removalNotification.wasEvicted()) {
            CompletableFuture<Optional<PublicProfile>> future = (CompletableFuture<Optional<PublicProfile>>)removalNotification.getValue();
            future.complete(Optional.empty());
        }
    }).build();

    private final Cache<UUID, PublicProfile> profiles = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public IdentityApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * Resolve a player by their minecraft player ID
     * @param playerId of the minecraft player
     * @return player
     */
    public CompletableFuture<Optional<Player>> resolvePlayer(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<ClientIdentity> identityOptional;
            try {
                identityOptional = identify(playerId).get(1, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                identityOptional = Optional.empty();
            }
            return identityOptional.map(identity -> new Player(identity.owner, new MinecraftPlayer(
                    playerId,
                    collar.player().minecraftPlayer.server,
                    collar.player().minecraftPlayer.networkId))
            );
        });
    }

    /**
     * Resolve a players profile using the player object
     * @param player to find the profile of
     * @return profile
     */
    public CompletableFuture<Optional<PublicProfile>> resolveProfile(Player player) {
        PublicProfile profile = profiles.getIfPresent(player.profile);
        if (profile == null) {
            CompletableFuture<Optional<PublicProfile>> future = new CompletableFuture<>();
            profileFutures.put(player.profile, future);
            sender.accept(new GetProfileRequest(identity(), player.profile));
            return future;
        } else {
            return CompletableFuture.completedFuture(Optional.of(profile));
        }
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

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
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
        } else if (resp instanceof GetProfileResponse) {
            GetProfileResponse response = (GetProfileResponse) resp;
            CompletableFuture<Optional<PublicProfile>> future = profileFutures.getIfPresent(response.profile.id);
            profileFutures.invalidate(response.id);
            if (future != null) {
                if (response.profile == null) {
                    future.complete(Optional.empty());
                } else {
                    future.complete(Optional.of(response.profile));
                }
            }
            return true;
        }
        return false;
    }
}
