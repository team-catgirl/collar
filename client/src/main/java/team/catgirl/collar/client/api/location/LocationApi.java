package team.catgirl.collar.client.api.location;

import com.google.common.hash.Hashing;
import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.api.entities.EntityType;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.api.groups.GroupsListener;
import team.catgirl.collar.client.minecraft.Ticks;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.location.*;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LocationApi extends AbstractApi<LocationListener> {

    private final HashSet<UUID> groupsSharingWith = new HashSet<>();
    private final ConcurrentHashMap<MinecraftPlayer, Location> playerLocations = new ConcurrentHashMap<>();
    private final Supplier<Location> locationSupplier;
    private final LocationUpdater updater;
    private final NearbyUpdater nearbyUpdater;
    private final Supplier<Set<Entity>> entityListSupplier;

    public LocationApi(Collar collar,
                       Supplier<ClientIdentityStore> identityStoreSupplier,
                       Consumer<ProtocolRequest> sender,
                       Ticks ticks,
                       GroupsApi groupsApi,
                       Supplier<Location> locationSupplier,
                       Supplier<Set<Entity>> entityListSupplier) {
        super(collar, identityStoreSupplier, sender);
        this.locationSupplier = locationSupplier;
        this.updater = new LocationUpdater(this, ticks);
        this.nearbyUpdater = new NearbyUpdater(entityListSupplier, this, ticks);
        this.entityListSupplier = entityListSupplier;
        groupsApi.subscribe(new GroupListenerImpl());
    }

    public Map<MinecraftPlayer, Location> playerLocations() {
        return new HashMap<>(playerLocations);
    }

    /**
     * Start sharing your coordinates with a group
     * @param group to share with
     */
    public void startSharingWith(Group group) {
        // Start sharing
        if (!this.updater.isRunning()) {
            this.updater.start();
        }
        synchronized (this) {
            groupsSharingWith.add(group.id);
            sender.accept(new StartSharingLocationRequest(identity(), group.id));
            if (!updater.isRunning()) {
                updater.start();
            }
        }
    }

    /**
     * Start sharing your coordinates with a group
     * @param group to stop sharing with
     */
    public void stopSharingWith(Group group) {
        synchronized (this) {
            stopSharingForGroup(group);
            sender.accept(new StopSharingLocationRequest(identity(), group.id));
        }
    }

    /**
     * Tests if you are currently sharing with the group
     * @param group to test
     * @return sharing
     */
    public boolean isSharingWith(Group group) {
        synchronized (this) {
            return groupsSharingWith.contains(group.id);
        }
    }

    private void stopSharingForGroup(Group group) {
        synchronized (this) {
            if (updater.isRunning() && groupsSharingWith.contains(group.id) && (groupsSharingWith.size() - 1) == 0) {
                updater.stop();
            }
            groupsSharingWith.remove(group.id);
        }
    }

    void publishLocation() {
        if (!groupsSharingWith.isEmpty()) {
            Location location = locationSupplier.get();
            byte[] bytes;
            try {
                bytes = location.serialize();
            } catch (IOException e) {
                throw new IllegalStateException("Could not serialize location " + location);
            }
            groupsSharingWith.forEach(groupId -> {
                collar.groups().findGroupById(groupId).ifPresent(group -> {
                    byte[] encryptedBytes = identityStore().createCypher().crypt(identity(), group, bytes);
                    sender.accept(new UpdateLocationRequest(identity(), groupId, encryptedBytes));
                });
            });
        }
    }

    void publishNearby(Set<Entity> entities) {
        Set<String> nearbyHashes = entities.stream().filter(entity -> entity.type.equals(EntityType.PLAYER))
                .limit(200)
                .map(entity -> Hashing.sha256().hashString(entity.id.toString(), StandardCharsets.UTF_8).toString())
                .collect(Collectors.toSet());
        sender.accept(new UpdateNearbyRequest(identity(), nearbyHashes));
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof LocationUpdatedResponse) {
            LocationUpdatedResponse response = (LocationUpdatedResponse) resp;
            synchronized (this) {
                collar.groups().findGroupById(response.group).ifPresent(group -> {
                    Location location;
                    if (response.location == null) {
                        // Stopped sharing
                        location = Location.UNKNOWN;
                    } else {
                        byte[] decryptedBytes = identityStore().createCypher().decrypt(response.sender, group, response.location);
                        try {
                            location = new Location(decryptedBytes);
                        } catch (IOException e) {
                            throw new IllegalStateException("could not decrypt location sent by " + response.sender);
                        }
                    }
                    if (location.equals(Location.UNKNOWN)) {
                        // Remove if stooped sharing
                        playerLocations.remove(response.player);
                    } else {
                        // Update the location
                        playerLocations.put(response.player, location);
                    }
                    fireListener("onLocationUpdated", locationListener -> {
                        locationListener.onLocationUpdated(collar, this, response.player, location);
                    });
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.CONNECTED) {
            nearbyUpdater.start();
        } else if (state == Collar.State.DISCONNECTED) {
            synchronized (this) {
                playerLocations.clear();
                groupsSharingWith.clear();
            }
            nearbyUpdater.stop();
        }
    }

    class GroupListenerImpl implements GroupsListener {
        @Override
        public void onGroupLeft(Collar collar, GroupsApi groupsApi, Group group, MinecraftPlayer player) {
            stopSharingForGroup(group);
        }
    }
}
