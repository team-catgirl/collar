package team.catgirl.collar.client;

import team.catgirl.collar.messages.ServerMessage.*;
import team.catgirl.collar.security.ServerIdentity;

public interface CollarListener {
    default void onConnected(CollarClient client, ServerIdentity serverIdentity) {};
    default void onIdentityCreated(CollarClient client, CreateIdentityResponse resp) {};
    default void onSessionCreated(CollarClient client) {}
    default void onDisconnect(CollarClient client) {}
    default void onGroupCreated(CollarClient client, CreateGroupResponse resp) {};
    default void onGroupMembershipRequested(CollarClient client, GroupMembershipRequest resp) {};
    default void onGroupJoined(CollarClient client, AcceptGroupMembershipResponse resp) {};
    default void onGroupLeft(CollarClient client, LeaveGroupResponse resp) {};
    default void onGroupUpdated(CollarClient client, UpdatePlayerStateResponse resp) {};
    default void onGroupInvitesSent(CollarClient client, GroupInviteResponse resp) {};
    default void onPongReceived(PongResponse pongResponse) {};
}
