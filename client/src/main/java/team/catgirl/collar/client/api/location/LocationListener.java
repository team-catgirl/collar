package team.catgirl.collar.client.api.location;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.ApiListener;

import java.util.Set;

public interface LocationListener extends ApiListener {
    /**
     * Fired when a player location was updated
     * @param collar client
     * @param locationApi api
     * @param player that is sharing their location
     * @param location of the player
     */
    default void onLocationUpdated(Collar collar, LocationApi locationApi, Player player, Location location) {};

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

    /**
     * Fired when private waypoints are received from the server.
     * @param collar client
     * @param locationApi api
     * @param privateWaypoints received
     */
    default void onPrivateWaypointsReceived(Collar collar, LocationApi locationApi, Set<Waypoint> privateWaypoints) {};
}
