package team.catgirl.collar.client.api;

import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractApi<T extends ApiListener> {

    private static final Logger LOGGER = Logger.getLogger(AbstractApi.class.getName());

    private final Set<T> listeners = new HashSet<>();
    protected final Collar collar;
    private final Supplier<ClientIdentityStore> identityStoreSupplier;
    protected final Consumer<ProtocolRequest> sender;

    public AbstractApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        this.collar = collar;
        this.identityStoreSupplier = identityStoreSupplier;
        this.sender = sender;
    }

    protected ClientIdentity identity() {
        ClientIdentityStore clientIdentityStore = identityStoreSupplier.get();
        if (clientIdentityStore == null) {
            throw new IllegalStateException("Client is not ready");
        }
        return identityStoreSupplier.get().currentIdentity();
    }

    protected ClientIdentityStore identityStore() {
        return identityStoreSupplier.get();
    }

    public void subscribe(T listener) {
        listeners.add(listener);
    }

    public void unsubscribe(T listener) {
        listeners.remove(listener);
    }

    protected void fireListener(String name, Consumer<T> listener) {
        LOGGER.log(Level.INFO, "Firing " + name + " listeners");
        listeners.forEach(t -> {
            try {
                listener.accept(t);
            } catch (Throwable e) {
                LOGGER.log(Level.INFO, "Listener threw exception", e);
            }
        });
    }

    /**
     * Listen to state changes from the client
     * @param state state
     */
    public abstract void onStateChanged(Collar.State state);

    /**
     * Handle a protocol response
     * @param resp to handle
     * @return if the response was handled by this module or not
     */
    public abstract boolean handleResponse(ProtocolResponse resp);
}
