package team.catgirl.collar.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.models.Group;
import team.catgirl.collar.security.ServerIdentity;

import java.util.List;
import java.util.UUID;

public class ServerMessage {
    @JsonProperty("serverConnectedResponse")
    public final ServerConnectedResponse serverConnectedResponse;
    @JsonProperty("identificationSuccessful")
    public final IdentificationSuccessful identificationSuccessful;
    @JsonProperty("createGroupResponse")
    public final CreateGroupResponse createGroupResponse;
    @JsonProperty("groupMembershipRequest")
    public final GroupMembershipRequest groupMembershipRequest;
    @JsonProperty("acceptGroupMembershipResponse")
    public final AcceptGroupMembershipResponse acceptGroupMembershipResponse;
    @JsonProperty("leaveGroupResponse")
    public final LeaveGroupResponse leaveGroupResponse;
    @JsonProperty("updatePlayerStateResponse")
    public final UpdatePlayerStateResponse updatePlayerStateResponse;
    @JsonProperty("pong")
    public final Pong pong;
    @JsonProperty("groupInviteResponse")
    public final GroupInviteResponse groupInviteResponse;

    public ServerMessage(
            @JsonProperty("serverConnectedResponse") ServerConnectedResponse serverConnectedResponse,
            @JsonProperty("identificationSuccessful") IdentificationSuccessful identificationSuccessful,
            @JsonProperty("createGroupResponse") CreateGroupResponse createGroupResponse,
            @JsonProperty("groupMembershipRequest") GroupMembershipRequest groupMembershipRequest,
            @JsonProperty("acceptGroupMembershipResponse") AcceptGroupMembershipResponse acceptGroupMembershipResponse,
            @JsonProperty("leaveGroupResponse") LeaveGroupResponse leaveGroupResponse,
            @JsonProperty("updatePlayerStateResponse") UpdatePlayerStateResponse updatePlayerStateResponse,
            @JsonProperty("pong") Pong pong,
            @JsonProperty("groupInviteResponse") GroupInviteResponse groupInviteResponse) {
        this.serverConnectedResponse = serverConnectedResponse;
        this.identificationSuccessful = identificationSuccessful;
        this.createGroupResponse = createGroupResponse;
        this.groupMembershipRequest = groupMembershipRequest;
        this.acceptGroupMembershipResponse = acceptGroupMembershipResponse;
        this.leaveGroupResponse = leaveGroupResponse;
        this.updatePlayerStateResponse = updatePlayerStateResponse;
        this.pong = pong;
        this.groupInviteResponse = groupInviteResponse;
    }

    public static final class ServerConnectedResponse {
        @JsonProperty("serverIdentity")
        public final ServerIdentity serverIdentity;
        public final String token;

        public ServerConnectedResponse(@JsonProperty("serverIdentity") ServerIdentity serverIdentity, String token) {
            this.serverIdentity = serverIdentity;
            this.token = token;
        }

        public ServerMessage serverMessage() {
            return new ServerMessage(this,null, null, null, null, null, null, null, null);
        }
    }

    public static final class Pong {
        @JsonIgnore
        public ServerMessage serverMessage() {
            return new ServerMessage(null, null, null, null, null, null, null, this, null);
        }
    }

    public static final class IdentificationSuccessful {
        public IdentificationSuccessful() {}

        @JsonIgnore
        public ServerMessage serverMessage() {
            return new ServerMessage(null, this, null, null, null, null, null, null, null);
        }
    }

    public static final class GroupMembershipRequest {
        @JsonProperty("groupId")
        public final String groupId;
        @JsonProperty("requester")
        public final UUID requester;
        @JsonProperty("members")
        public final List<UUID> members;

        public GroupMembershipRequest(@JsonProperty("groupId") String groupId, @JsonProperty("requester") UUID requester, @JsonProperty("members") List<UUID> members) {
            this.groupId = groupId;
            this.requester = requester;
            this.members = members;
        }

        @JsonIgnore
        public ServerMessage serverMessage() {
            return new ServerMessage(null, null, null, this, null, null, null, null, null);
        }
    }

    public static final class CreateGroupResponse {
        @JsonProperty("group")
        public final Group group;

        public CreateGroupResponse(@JsonProperty("group") Group group) {
            this.group = group;
        }

        @JsonIgnore
        public ServerMessage serverMessage() {
            return new ServerMessage(null, null, this, null, null, null, null, null, null);
        }
    }

    public static final class AcceptGroupMembershipResponse {
        @JsonProperty("group")
        public final Group group;

        public AcceptGroupMembershipResponse(@JsonProperty("group") Group group) {
            this.group = group;
        }

        @JsonIgnore
        public ServerMessage serverMessage() {
            return new ServerMessage(null, null, null, null, this, null, null, null, null);
        }
    }

    public static final class LeaveGroupResponse {
        @JsonProperty("groupId")
        public final String groupId;

        public LeaveGroupResponse(@JsonProperty("groupId") String groupId) {
            this.groupId = groupId;
        }

        @JsonIgnore
        public ServerMessage serverMessage() {
            return new ServerMessage(null, null, null, null, null, this, null, null, null);
        }
    }

    public static final class UpdatePlayerStateResponse {
        @JsonProperty("groups")
        public final List<Group> groups;

        public UpdatePlayerStateResponse(@JsonProperty("groups") List<Group> groups) {
            this.groups = groups;
        }

        @JsonIgnore
        public ServerMessage serverMessage() {
            return new ServerMessage(null, null, null, null, null, null, this, null, null);
        }
    }

    public static final class GroupInviteResponse {
        @JsonProperty("groupId")
        public final String groupId;
        @JsonProperty("players")
        public final List<UUID> players;

        public GroupInviteResponse(@JsonProperty("groupId") String groupId, List<UUID> players) {
            this.groupId = groupId;
            this.players = players;
        }

        @JsonIgnore
        public ServerMessage serverMessage() {
            return new ServerMessage(null, null, null, null, null, null, null, null, this);
        }
    }
}
