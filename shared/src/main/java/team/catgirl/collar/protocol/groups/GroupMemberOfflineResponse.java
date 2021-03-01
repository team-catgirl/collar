package team.catgirl.collar.protocol.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

import java.util.UUID;

public final class GroupMemberOfflineResponse extends ProtocolResponse {
    @JsonProperty("groupId")
    public final UUID groupId;
    @JsonProperty("sender")
    public final ClientIdentity sender;

    public GroupMemberOfflineResponse(@JsonProperty("identity") ServerIdentity serverIdentity,
                                      @JsonProperty("groupId") UUID groupId,
                                      @JsonProperty("sender") ClientIdentity sender) {
        super(serverIdentity);
        this.groupId = groupId;
        this.sender = sender;
    }
}
