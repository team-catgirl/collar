package team.catgirl.collar.client.api.location;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.ApiListener;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public interface LocationListener extends ApiListener {
    /**
     * Fired when a player location was updated
     * @param collar client
     * @param locationApi api
     * @param player that is sharing their location
     * @param location of the player
     */
    default void onLocationUpdated(Collar collar, LocationApi locationApi, MinecraftPlayer player, Location location) {};

    /**
     * Fired when a waypoint was created
     * @param collar client
     * @param locationApi api
     * @param group the waypoint belong to or null if private waypoint
     * @param waypoint that was removed
     */
    default void onWaypointCreated(Collar collar, LocationApi locationApi, Group group, Waypoint waypoint) {};

    /**
     * Fired when a waypoint was removed
     * @param collar client
     * @param locationApi api
     * @param group the waypoint belong to or null if private waypoint
     * @param waypoint that was removed
     */
    default void onWaypointRemoved(Collar collar, LocationApi locationApi, Group group, Waypoint waypoint) {};
}
