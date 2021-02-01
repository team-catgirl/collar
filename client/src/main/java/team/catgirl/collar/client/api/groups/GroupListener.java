package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.features.ApiListener;

public interface GroupListener extends ApiListener {
    default void onGroupCreated(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupJoined(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupLeft(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupMemberPositionUpdated(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupMemberInvitationsSent(Collar collar, GroupsFeature feature, Group group) {};
    default void onGroupInvited(Collar collar, GroupsFeature feature, GroupInvitation invitation) {};
}
