package team.catgirl.collar.server.protocol;

import com.google.common.collect.ImmutableMap;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

import java.util.HashMap;
import java.util.Map;

public final class BatchProtocolResponse extends ProtocolResponse {
    public final Map<ProtocolResponse, ClientIdentity> responses;

    public BatchProtocolResponse(ServerIdentity identity, Map<ProtocolResponse, ClientIdentity> responses) {
        super(identity);
        this.responses = responses;
    }

    public BatchProtocolResponse(ServerIdentity identity) {
        this(identity, new HashMap<>());
    }

    public BatchProtocolResponse add(ClientIdentity identity, ProtocolResponse response) {
        Map<ProtocolResponse, ClientIdentity> responsesCopy = new HashMap<>();
        responsesCopy.put(response, identity);
        responsesCopy.putAll(responses);
        return new BatchProtocolResponse(this.identity, responsesCopy);
    }

    public BatchProtocolResponse concat(BatchProtocolResponse response) {
        Map<ProtocolResponse, ClientIdentity> responsesCopy = new HashMap<>();
        responsesCopy.putAll(responses);
        responsesCopy.putAll(response.responses);
        return new BatchProtocolResponse(identity, responsesCopy);
    }

    public static BatchProtocolResponse one(ClientIdentity identity, ProtocolResponse response) {
        return new BatchProtocolResponse(response.identity, ImmutableMap.of(response, identity));
    }
}
