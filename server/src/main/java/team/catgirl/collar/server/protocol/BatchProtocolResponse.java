package team.catgirl.collar.server.protocol;

import com.google.common.collect.ImmutableMap;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class BatchProtocolResponse extends ProtocolResponse {
    public final Map<ProtocolResponse, ClientIdentity> responses;

    public BatchProtocolResponse(ServerIdentity identity, Map<ProtocolResponse, ClientIdentity> responses) {
        super(identity);
        this.responses = responses;
    }

    public BatchProtocolResponse(ServerIdentity identity) {
        this(identity, new LinkedHashMap<>());
    }

    public BatchProtocolResponse add(ClientIdentity identity, ProtocolResponse response) {
        responses.put(response, identity);
        return this;
    }

    public BatchProtocolResponse concat(BatchProtocolResponse response) {
        responses.putAll(response.responses);
        return this;
    }

    public static BatchProtocolResponse one(ClientIdentity identity, ProtocolResponse response) {
        return new BatchProtocolResponse(response.identity, ImmutableMap.of(response, identity));
    }

    public Optional<BatchProtocolResponse> optional() {
        return responses.isEmpty() ? Optional.empty() : Optional.of(this);
    }
}
