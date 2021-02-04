package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.waypoints.Waypoint;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.features.ApiListener;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public interface GroupListener extends ApiListener {
    default void onGroupCreated(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupJoined(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupLeft(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupsUpdated(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupMemberInvitationsSent(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupInvited(Collar collar, GroupsFeature feature, GroupInvitation invitation) {};
    default void onGroupMemberRemoved(Collar collar, Group group, MinecraftPlayer player) {};

    ///
    /// Waypoint listeners
    ///
    default void onWaypointCreatedSuccess(Collar collar, GroupsFeature feature, Group group, Waypoint waypoint) {};
    default void onWaypointCreatedFailed(Collar collar, GroupsFeature feature, Group group, String name) {};
    default void onWaypointRemovedSuccess(Collar collar, GroupsFeature feature, Group group, Waypoint waypoint) {};
    default void onWaypointRemovedFailed(Collar collar, GroupsFeature feature, Group group, Waypoint waypoint) {};
}
