package team.catgirl.collar.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import team.catgirl.collar.models.Group.MembershipState;
import team.catgirl.collar.security.PlayerIdentity;
import team.catgirl.collar.models.Position;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ClientMessage {
    @JsonProperty("identity")
    public final PlayerIdentity identity;
    @JsonProperty("createIdentityRequest")
    public final CreateIdentityRequest createIdentityRequest;
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
            @JsonProperty("identity") PlayerIdentity identity,
            @JsonProperty("createIdentityRequest") CreateIdentityRequest createIdentityRequest, @JsonProperty("identify") IdentifyRequest identifyRequest,
            @JsonProperty("createGroupRequest") CreateGroupRequest createGroupRequest,
            @JsonProperty("groupMembershipRequest") AcceptGroupMembershipRequest acceptGroupMembershipRequest,
            @JsonProperty("leaveGroupRequest") LeaveGroupRequest leaveGroupRequest,
            @JsonProperty("updatePlayerStateRequest") UpdatePlayerStateRequest updatePlayerStateRequest,
            @JsonProperty("ping") Ping ping,
            @JsonProperty("groupInviteRequest") GroupInviteRequest groupInviteRequest) {
        this.identity = identity;
        this.createIdentityRequest = createIdentityRequest;
        this.identifyRequest = identifyRequest;
        this.createGroupRequest = createGroupRequest;
        this.acceptGroupMembershipRequest = acceptGroupMembershipRequest;
        this.leaveGroupRequest = leaveGroupRequest;
        this.updatePlayerStateRequest = updatePlayerStateRequest;
        this.ping = ping;
        this.groupInviteRequest = groupInviteRequest;
    }

    public static final class Ping {
        public ClientMessage clientMessage(PlayerIdentity identity) {
            return new ClientMessage(identity, null, null, null, null, null, null, this, null);
        }
    }

    public static final class IdentifyRequest {
        public IdentifyRequest() {}

        public ClientMessage clientMessage(PlayerIdentity identity) {
            return new ClientMessage(identity, null, this, null, null, null, null, null, null);
        }
    }

    public static final class CreateGroupRequest {
        @JsonProperty("players")
        public final List<UUID> players;
        @JsonProperty("position")
        public Position position;

        public CreateGroupRequest(@JsonProperty("players") List<UUID> players, @JsonProperty("position") Position position) {
            this.players = players;
        }

        public ClientMessage clientMessage(PlayerIdentity identity) {
            return new ClientMessage(identity, null, null, this, null, null, null, null, null);
        }
    }

    public static final class AcceptGroupMembershipRequest {
        @JsonProperty("groupId")
        public final String groupId;
        @JsonProperty("state")
        public final MembershipState state;

        public AcceptGroupMembershipRequest(
                @JsonProperty("groupId") String groupId,
                @JsonProperty("state") MembershipState state) {
            this.groupId = groupId;
            this.state = state;
        }

        public static enum Status {
            ACCEPT,
            DECLINE
        }

        public ClientMessage clientMessage(PlayerIdentity identity) {
            return new ClientMessage(identity, null, null, null, this, null, null, null, null);
        }
    }

    public static final class LeaveGroupRequest {
        @JsonProperty("groupId")
        public final String groupId;

        public LeaveGroupRequest(@JsonProperty("groupId") String groupId) {
            this.groupId = groupId;
        }

        public ClientMessage clientMessage(PlayerIdentity identity) {
            return new ClientMessage(identity, null, null, null, null, this, null, null, null);
        }
    }

    public static final class UpdatePlayerStateRequest {
        @JsonProperty("position")
        public final Position position;

        public UpdatePlayerStateRequest(@JsonProperty("position") Position position) {
            this.position = position;
        }

        public ClientMessage clientMessage(PlayerIdentity identity) {
            return new ClientMessage(identity, null, null, null, null, null, this, null, null);
        }
    }

    public static final class GroupInviteRequest {
        @JsonProperty("groupId")
        public final String groupId;
        @JsonProperty("players")
        public final List<UUID> players;

        public GroupInviteRequest(@JsonProperty("groupId") String groupId, @JsonProperty("players") List<UUID> players) {
            this.groupId = groupId;
            this.players = players;
        }

        public ClientMessage clientMessage(PlayerIdentity identity) {
            return new ClientMessage(identity, null, null, null, null, null, null, null, this);
        }
    }

    public static class CreateIdentityRequest {
        @JsonProperty("signedPreKey")
        public final byte[] signedPreKey;
        @JsonProperty("preKeys")
        public final List<byte[]> preKeys;

        public CreateIdentityRequest(@JsonProperty("signedPreKey") byte[] signedPreKey, @JsonProperty("preKeys") List<byte[]> preKeys) {
            this.signedPreKey = signedPreKey;
            this.preKeys = preKeys;
        }

        public static CreateIdentityRequest from(SignedPreKeyRecord signedPreKeyRecord, List<PreKeyRecord> preKeyRecords) {
            return new CreateIdentityRequest(signedPreKeyRecord.serialize(), preKeyRecords.stream().map(PreKeyRecord::serialize).collect(Collectors.toList()));
        }

        public ClientMessage clientMessage(PlayerIdentity identity) {
            return new ClientMessage(identity, this, null, null, null, null, null, null, null);
        }
    }
}
