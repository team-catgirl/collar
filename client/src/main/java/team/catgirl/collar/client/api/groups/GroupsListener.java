package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.features.ApiListener;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public interface GroupsListener extends ApiListener {
    default void onGroupCreated(Collar collar, GroupsApi groupsApi, Group group) {};
    default void onGroupJoined(Collar collar, GroupsApi groupsApi, Group group) {};
    default void onGroupLeft(Collar collar, GroupsApi groupsApi, Group group) {};
    default void onGroupUpdated(Collar collar, GroupsApi groupsApi, Group group) {};
    default void onGroupMemberInvitationsSent(Collar collar, GroupsApi groupsApi, Group group) {};
    default void onGroupInvited(Collar collar, GroupsApi groupsApi, GroupInvitation invitation) {};
    default void onGroupMemberRemoved(Collar collar, Group group, MinecraftPlayer player) {}
}
