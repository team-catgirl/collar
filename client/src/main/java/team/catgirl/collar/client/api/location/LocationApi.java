package team.catgirl.collar.client.api.location;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.features.AbstractApi;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.api.groups.GroupsListener;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.location.LocationUpdatedResponse;
import team.catgirl.collar.protocol.location.StartSharingLocationRequest;
import team.catgirl.collar.protocol.location.StopSharingLocationRequest;
import team.catgirl.collar.protocol.location.UpdateLocationRequest;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LocationApi extends AbstractApi<LocationListener> {

    private final HashSet<UUID> groupsSharingWith = new HashSet<>();
    private final ConcurrentHashMap<MinecraftPlayer, Location> playerLocations = new ConcurrentHashMap<>();
    private final Supplier<Location> locationSupplier;
    private final LocationUpdater updater;

    public LocationApi(Collar collar,
                       Supplier<ClientIdentityStore> identityStoreSupplier,
                       Consumer<ProtocolRequest> sender,
                       GroupsApi groupsApi,
                       Supplier<Location> locationSupplier) {
        super(collar, identityStoreSupplier, sender);
        this.locationSupplier = locationSupplier;
        this.updater = new LocationUpdater(this);
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
            sender.accept(new UpdateLocationRequest(identity(), locationSupplier.get()));
        }
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof LocationUpdatedResponse) {
            LocationUpdatedResponse response = (LocationUpdatedResponse) resp;
            synchronized (this) {
                if (response.location.equals(Location.UNKNOWN)) {
                    playerLocations.remove(response.player);
                } else {
                    playerLocations.put(response.player, response.location);
                }
                fireListener("onLocationUpdated", locationListener -> {
                    locationListener.onLocationUpdated(collar, this, response.player, response.location);
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public void onStageChanged(Collar.State state) {
        if (state == Collar.State.DISCONNECTED) {
            synchronized (this) {
                playerLocations.clear();
                groupsSharingWith.clear();
            }
        }
    }

    class GroupListenerImpl implements GroupsListener {
        @Override
        public void onGroupLeft(Collar collar, GroupsApi groupsApi, Group group, MinecraftPlayer player) {
            stopSharingForGroup(group);
        }
    }
}
