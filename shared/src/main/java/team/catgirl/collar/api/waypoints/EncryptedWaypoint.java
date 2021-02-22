package team.catgirl.collar.api.waypoints;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.security.ClientIdentity;

/**
 * Encrypted {@link Waypoint}
 */
public final class EncryptedWaypoint {
    /**
     * The identity that created the waypoint and who's keys can decode it
     */
    @JsonProperty("sender")
    public final ClientIdentity sender;

    /**
     * The encrypted {@link Waypoint} data
     */
    @JsonProperty("waypoint")
    public final byte[] waypoint;

    public EncryptedWaypoint(@JsonProperty("sender") ClientIdentity sender,
                             @JsonProperty("waypoint") byte[] waypoint) {
        this.sender = sender;
        this.waypoint = waypoint;
    }
}
