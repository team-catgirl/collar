package team.catgirl.collar.client;

import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.messages.ServerMessage.*;
import team.catgirl.collar.models.Group;
import team.catgirl.collar.models.Position;
import team.catgirl.collar.security.ServerIdentity;

import java.util.List;

public interface CollarListener {
    /**
     * Client has a successful connected to the server.
     * Do not override unless you want to control the client/server verification process.
     * If you want to hook into when collar is successfully setup, use {@link #onSessionCreated(CollarClient)}
     * @param client instance
     * @param response the connection response
     */
    default void onConnected(CollarClient client, ServerConnectedResponse response) {};

    /**
     * Fired when {@link #onConnected(CollarClient, ServerConnectedResponse)} detects that the server fingerprint has changed
     * If its changed, then you might not be able to trust the server.
     * Either add {@link ServerIdentity} in the {@link team.catgirl.collar.client.security.ServerIdentityStore} and then call {@link CollarClient#identify()}
     * or {@link CollarClient#disconnect()}
     * @param client instance
     * @param serverIdentity of the collar server
     */
    default void onFingerPrintMismatch(CollarClient client, ServerIdentity serverIdentity) {};

    /**
     * Fired when {@link #onConnected(CollarClient, ServerConnectedResponse)} detects that the server has been seen for the first time
     * You should add {@link ServerIdentity} in the {@link team.catgirl.collar.client.security.ServerIdentityStore}
     * and call {@link CollarClient#identify()} otherwise {@link CollarClient#disconnect()}
     * @param client instance
     * @param serverIdentity of the collar server
     */
    default void onNewServerIdentity(CollarClient client, ServerIdentity serverIdentity) {};

    /**
     * Fired when the session with server has been established.
     * You can now send messages to the server and get back responses!
     * @param client instance
     */
    default void onSessionCreated(CollarClient client) {}

    /**
     * Fired when the client is disconnected from the server
     * Your reconnect logic goes here
     * @param client instance
     */
    default void onDisconnect(CollarClient client) {}

    /**
     * Fired when the server creates the requested group using {@link CollarClient#createGroup(List, Position)}
     * @param client instance
     * @param resp response, including the newly created group
     */
    default void onGroupCreated(CollarClient client, CreateGroupResponse resp) {};

    /**
     * Fired when another client requests that this client becomes a member of the group
     * Respond with {@link CollarClient#acceptGroupRequest(String, Group.MembershipState)}
     * @param client instance
     * @param req membership request info
     */
    default void onGroupMembershipRequested(CollarClient client, GroupMembershipRequest req) {};

    /**
     * Fired when you join a group. This is usually sent after {@link CollarClient#acceptGroupRequest(String, Group.MembershipState)}
     * @param client instance
     * @param resp with current group state
     */
    default void onGroupJoined(CollarClient client, AcceptGroupMembershipResponse resp) {};

    /**
     * Fired when the client has been removed from the group
     * Usually sent when {@link CollarClient#leaveGroup(Group)} has been sent
     * @param client instance
     * @param resp of the group you left
     */
    default void onGroupLeft(CollarClient client, LeaveGroupResponse resp) {};

    /**
     * Fired when the server detects a change in group state. For example, members leaving and joining.
     * @param client instance
     * @param resp with all groups you are a member of
     */
    default void onGroupUpdated(CollarClient client, UpdatePlayerStateResponse resp) {};

    /**
     * Fired when the server successfully invites members when the client uses {@link CollarClient#invite(Group, List)}
     * @param client instance
     * @param resp containing the group id and who was successfully invited
     */
    default void onGroupInvitesSent(CollarClient client, GroupInviteResponse resp) {};

    /**
     * Fired when the clients ping request is pong'd by the server
     * Do not override unless you want to control this for some reason
     * @param pong message
     */
    default void onPongReceived(ServerMessage.Pong pong) {};
}
