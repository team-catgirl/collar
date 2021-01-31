package team.catgirl.collar.client.api.features;

import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractFeature<T extends ApiListener> {
    private final Set<T> listeners = new HashSet<>();

    public void subscribe(T listener) {
        listeners.add(listener);
    }

    public void unsubscribe(T listener) {
        listeners.remove(listener);
    }

    public abstract boolean handleProtocolResponse(ProtocolResponse resp, Consumer<ProtocolRequest> sender);
}
