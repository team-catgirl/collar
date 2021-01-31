package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.client.api.features.AbstractFeature;
import team.catgirl.collar.models.Group;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public final class GroupsFeature extends AbstractFeature<GroupListener> {
    private final ConcurrentMap<String, Group> groups = new ConcurrentHashMap<>();

    @Override
    public boolean handleProtocolResponse(ProtocolResponse resp, Consumer<ProtocolRequest> sender) {
        return false;
    }
}
