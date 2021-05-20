package team.catgirl.collar.client.api.location;

import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.api.entities.EntityType;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.api.groups.GroupsListener;
import team.catgirl.collar.client.minecraft.Ticks;
import team.catgirl.collar.client.sdht.SDHTApi;
import team.catgirl.collar.client.sdht.SDHTListener;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.location.*;
import team.catgirl.collar.protocol.waypoints.CreateWaypointRequest;
import team.catgirl.collar.protocol.waypoints.GetWaypointsRequest;
import team.catgirl.collar.protocol.waypoints.GetWaypointsResponse;
import team.catgirl.collar.protocol.waypoints.RemoveWaypointRequest;
import team.catgirl.collar.sdht.Content;
import team.catgirl.collar.sdht.Key;
import team.catgirl.collar.security.cipher.CipherException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LocationApi extends AbstractApi<LocationListener> {

    private final HashSet<UUID> groupsSharingWith = new HashSet<>();
    private final ConcurrentHashMap<Player, Location> playerLocations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Waypoint> privateWaypoints = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Map<UUID, Waypoint>> groupWaypoints = new ConcurrentHashMap<>();
    private final Supplier<Location> locationSupplier;
    private final LocationUpdater updater;
    private final NearbyUpdater nearbyUpdater;
    private final SDHTApi sdhtApi;
    private final SDHTListenerImpl sdhtListener;

    public LocationApi(Collar collar,
                       Supplier<ClientIdentityStore> identityStoreSupplier,
                       Consumer<ProtocolRequest> sender,
                       Ticks ticks,
                       GroupsApi groupsApi,
                       SDHTApi sdhtApi,
                       Supplier<Location> locationSupplier,
                       Supplier<Set<Entity>> entityListSupplier) {
        super(collar, identityStoreSupplier, sender);
        this.locationSupplier = locationSupplier;
        this.updater = new LocationUpdater(this, ticks);
        this.nearbyUpdater = new NearbyUpdater(entityListSupplier, this, ticks);
        groupsApi.subscribe(new GroupListenerImpl());
        this.sdhtApi = sdhtApi;
        this.sdhtListener = new SDHTListenerImpl(this);
        this.sdhtApi.subscribe(sdhtListener);
    }

    /**
     * Players who are sharing their location with you
     * @return players to their locations
     */
    public Map<Player, Location> playerLocations() {
        return new HashMap<>(playerLocations);
    }

    /**
     * @return your private waypoints
     */
    public Set<Waypoint> privateWaypoints() {
        return ImmutableSet.copyOf(privateWaypoints.values());
    }

    /**
     * Waypoints shared with you via a group
     * @param group the waypoints belong to
     * @return waypoints
     */
    public Set<Waypoint> groupWaypoints(Group group) {
        Map<UUID, Waypoint> waypoints = groupWaypoints.get(group.id);
        if (waypoints == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.copyOf(waypoints.values());
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
        fireListener("onStartedSharingLocation", listener -> listener.onStartedSharingLocation(collar, this, group));
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
        fireListener("onStoppedSharingLocation", listener -> listener.onStoppedSharingLocation(collar, this, group));
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

    /**
     * Add a shared {@link team.catgirl.collar.api.waypoints.Waypoint} to the group
     * @param group to add waypoint to
     * @param name of the waypoint
     * @param location of the waypoint
     */
    public void addWaypoint(Group group, String name, Location location) {
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), name, location, collar.player().minecraftPlayer.server);
        groupWaypoints.compute(group.id, (groupId, waypointMap) -> {
            waypointMap = waypointMap == null ? new ConcurrentHashMap<>() : waypointMap;
            waypointMap.computeIfAbsent(waypoint.id, waypointId -> waypoint);
            return waypointMap;
        });
        Content content = Content.from(waypoint.serialize(), Waypoint.class);
        sdhtApi.table.put(new Key(group.id, waypoint.id), content);
        fireListener("onWaypointCreated", listener -> listener.onWaypointCreated(collar, this, group, waypoint));
    }

    /**
     * Remove a shared {@link Waypoint} from a group
     * @param group to the waypoint belongs to
     * @param waypoint the waypoint to remove
     */
    public void removeWaypoint(Group group, Waypoint waypoint) {
        AtomicReference<Waypoint> removedWaypoint = new AtomicReference<>();
        groupWaypoints.compute(group.id, (uuid, waypointMap) -> {
            if (waypointMap == null) {
                return null;
            }
            Waypoint removed = waypointMap.remove(waypoint.id);
            removedWaypoint.set(removed);
            if (waypointMap.isEmpty()) {
                return null;
            }
            return waypointMap;
        });
        sdhtApi.table.delete(new Key(group.id, waypoint.id));
        if (removedWaypoint.get() != null) {
            fireListener("onWaypointRemoved", listener -> listener.onWaypointRemoved(collar, this, group, waypoint));
        }
    }

    /**
     * Add a {@link Waypoint} to the players private waypoint list
     * @param name of the waypoint
     * @param location of the waypoint
     */
    public void addWaypoint(String name, Location location) {
        Waypoint waypoint = privateWaypoints.computeIfAbsent(UUID.randomUUID(), uuid -> new Waypoint(uuid, name, location, collar.player().minecraftPlayer.server));
        byte[] bytes = waypoint.serialize();
        byte[] encryptedBytes;
        try {
            encryptedBytes = identityStore().createCypher().crypt(bytes);
        } catch (CipherException e) {
            throw new IllegalStateException(e);
        }
        sender.accept(new CreateWaypointRequest(identity(), waypoint.id, encryptedBytes));
        fireListener("onWaypointCreated", listener -> listener.onWaypointCreated(collar, this, null, waypoint));
    }

    /**
     * Remove a {@link Waypoint} from the players private waypoint list
     * @param waypoint to remove
     */
    public void removeWaypoint(Waypoint waypoint) {
        privateWaypoints.remove(waypoint.id);
        sender.accept(new RemoveWaypointRequest(identity(), waypoint.id));
        fireListener("onWaypointRemoved", listener -> listener.onWaypointRemoved(collar, this, null, waypoint));
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
            byte[] bytes = location.serialize();
            groupsSharingWith.forEach(groupId -> {
                collar.groups().findGroupById(groupId).ifPresent(group -> {
                    byte[] encryptedBytes;
                    try {
                        encryptedBytes = identityStore().createCypher().crypt(identity(), group, bytes);
                    } catch (CipherException e) {
                        throw new IllegalStateException(e);
                    }
                    sender.accept(new UpdateLocationRequest(identity(), groupId, encryptedBytes));
                });
            });
        }
    }

    void publishNearby(Set<Entity> entities) {
        Set<String> nearbyHashes = entities.stream().filter(entity -> entity.isTypeOf(EntityType.PLAYER))
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
                        try {
                            byte[] decryptedBytes = identityStore().createCypher().decrypt(response.sender, group, response.location);
                            location = new Location(decryptedBytes);
                        } catch (IOException | CipherException e) {
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
                    fireListener("onLocationUpdated", listener -> listener.onLocationUpdated(collar, this, response.player, location));
                });
            }
            return true;
        } else if (resp instanceof GetWaypointsResponse) {
            GetWaypointsResponse response = (GetWaypointsResponse) resp;
            if (!response.waypoints.isEmpty()) {
                Map<UUID, Waypoint> waypoints = response.waypoints.stream()
                        .map(encryptedWaypoint -> {
                            try {
                                return identityStore().createCypher().decrypt(encryptedWaypoint.waypoint);
                            } catch (CipherException e) {
                                throw new IllegalStateException(e);
                            }
                        }).map(Waypoint::new)
                        .filter(waypoint -> waypoint.server.equals(collar.player().minecraftPlayer.server))
                        .collect(Collectors.toMap(o -> o.id, o -> o));
                privateWaypoints.putAll(waypoints);
                fireListener("onPrivateWaypointsReceived", listener -> listener.onPrivateWaypointsReceived(collar, this, ImmutableSet.copyOf(waypoints.values())));
            }
        }
        return false;
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.CONNECTED) {
            nearbyUpdater.start();
            sender.accept(new GetWaypointsRequest(identity()));
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
        public void onGroupLeft(Collar collar, GroupsApi groupsApi, Group group, Player player) {
            stopSharingForGroup(group);
        }
    }

    class SDHTListenerImpl implements SDHTListener {

        private final LocationApi locationApi;

        public SDHTListenerImpl(LocationApi locationApi) {
            this.locationApi = locationApi;
        }

        @Override
        public void onRecordAdded(Collar collar, SDHTApi sdhtApi, Key key, Content content) {
            if (Waypoint.class.equals(content.type)) {
                collar.groups().findGroupById(key.namespace).ifPresent(group -> {
                    AtomicReference<Waypoint> waypointAdded = new AtomicReference<>();
                    groupWaypoints.compute(key.namespace, (uuid, waypointMap) -> {
                        waypointMap = waypointMap == null ? new ConcurrentHashMap<>() : waypointMap;
                        Waypoint waypoint = new Waypoint(content.bytes);
                        waypointMap.put(key.id, waypoint);
                        waypointAdded.set(waypoint);
                        return waypointMap;
                    });
                    if (waypointAdded.get() != null) {
                        fireListener("onWaypointCreated", listener -> listener.onWaypointCreated(collar, locationApi, group, waypointAdded.get()));
                    }
                });
            }
        }

        @Override
        public void onRecordRemoved(Collar collar, SDHTApi sdhtApi, Key key, Content content) {
            if (Waypoint.class.equals(content.type)) {
                collar.groups().findGroupById(key.namespace).ifPresent(group -> {
                    AtomicReference<Waypoint> waypointRemoved = new AtomicReference<>();
                    groupWaypoints.compute(key.namespace, (uuid, waypointMap) -> {
                        if (waypointMap == null) {
                            return null;
                        }
                        Waypoint removed = waypointMap.remove(key.id);
                        waypointRemoved.set(removed);
                        return waypointMap.isEmpty() ? null : waypointMap;
                    });
                    if (waypointRemoved.get() != null) {
                        fireListener("onWaypointCreated", listener -> listener.onWaypointCreated(collar, locationApi, group, waypointRemoved.get()));
                    }
                });
            }
        }
    }
}
