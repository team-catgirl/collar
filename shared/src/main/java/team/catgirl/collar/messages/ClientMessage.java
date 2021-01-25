package team.catgirl.collar.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.models.Group.MembershipState;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.models.Position;

import java.util.List;
import java.util.UUID;

public final class ClientMessage {

    @JsonProperty("identify")
    public final IdentifyRequest identifyRequest;
    @JsonProperty("createGroupRequest")
    public final CreateGroupRequest createGroupRequest;
    @JsonProperty("acceptGroupMembershipRequest")
    public final AcceptGroupMembershipRequest acceptGroupMembershipRequest;
    @JsonProperty("leaveGroupRequest")
    public final LeaveGroupRequest leaveGroupRequest;
    @JsonProperty("updatePlayerStateRequest")
    public final UpdatePlayerStateRequest updatePlayerStateRequest;
    @JsonProperty("ping")
    public final Ping ping;
    @JsonProperty("groupInviteRequest")
    public final GroupInviteRequest groupInviteRequest;

    public ClientMessage(
            @JsonProperty("identify") IdentifyRequest identifyRequest,
            @JsonProperty("createGroupRequest") CreateGroupRequest createGroupRequest,
            @JsonProperty("groupMembershipRequest") AcceptGroupMembershipRequest acceptGroupMembershipRequest,
            @JsonProperty("leaveGroupRequest") LeaveGroupRequest leaveGroupRequest,
            @JsonProperty("updatePlayerStateRequest") UpdatePlayerStateRequest updatePlayerStateRequest,
            @JsonProperty("ping") Ping ping,
            @JsonProperty("groupInviteRequest") GroupInviteRequest groupInviteRequest) {
        this.identifyRequest = identifyRequest;
        this.createGroupRequest = createGroupRequest;
        this.acceptGroupMembershipRequest = acceptGroupMembershipRequest;
        this.leaveGroupRequest = leaveGroupRequest;
        this.updatePlayerStateRequest = updatePlayerStateRequest;
        this.ping = ping;
        this.groupInviteRequest = groupInviteRequest;
    }

    public static final class Ping {}

    public static final class IdentifyRequest {
        public final PlayerIdentity playerIdentity;
        public final String token;

        public IdentifyRequest(@JsonProperty("identity") PlayerIdentity playerIdentity, @JsonProperty("token") String token) {
            this.playerIdentity = playerIdentity;
            this.token = token;
        }
    }

    public static final class CreateGroupRequest {
        @JsonProperty("me")
        public final PlayerIdentity me;
        @JsonProperty("players")
        public final List<UUID> players;
        @JsonProperty("position")
        public Position position;

        public CreateGroupRequest(@JsonProperty("me") PlayerIdentity me, @JsonProperty("players") List<UUID> players, @JsonProperty("position") Position position) {
            this.me = me;
            this.players = players;
        }
    }

    public static final class AcceptGroupMembershipRequest {
        @JsonProperty("me")
        public final PlayerIdentity me;
        @JsonProperty("groupId")
        public final String groupId;
        @JsonProperty("state")
        public final MembershipState state;

        public AcceptGroupMembershipRequest(
                @JsonProperty("me") PlayerIdentity me,
                @JsonProperty("groupId") String groupId,
                @JsonProperty("state") MembershipState state) {
            this.me = me;
            this.groupId = groupId;
            this.state = state;
        }

        public static enum Status {
            ACCEPT,
            DECLINE
        }
    }

    public static final class LeaveGroupRequest {
        @JsonProperty("me")
        public final PlayerIdentity me;
        @JsonProperty("groupId")
        public final String groupId;

        public LeaveGroupRequest(@JsonProperty("me") PlayerIdentity me, @JsonProperty("groupId") String groupId) {
            this.me = me;
            this.groupId = groupId;
        }
    }

    public static final class UpdatePlayerStateRequest {
        @JsonProperty("me")
        public final PlayerIdentity me;
        @JsonProperty("position")
        public final Position position;

        public UpdatePlayerStateRequest(@JsonProperty("me") PlayerIdentity me, @JsonProperty("position") Position position) {
            this.me = me;
            this.position = position;
        }
    }

    public static final class GroupInviteRequest {
        @JsonProperty("me")
        public final PlayerIdentity me;
        @JsonProperty("groupId")
        public final String groupId;
        @JsonProperty("players")
        public final List<UUID> players;

        public GroupInviteRequest(@JsonProperty("me") PlayerIdentity me, @JsonProperty("groupId") String groupId, @JsonProperty("players") List<UUID> players) {
            this.me = me;
            this.groupId = groupId;
            this.players = players;
        }
    }
}
