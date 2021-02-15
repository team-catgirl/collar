package team.catgirl.collar.client.api.identity;

import com.google.common.util.concurrent.SettableFuture;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.identity.GetIdentityRequest;
import team.catgirl.collar.protocol.identity.GetIdentityResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.TokenGenerator;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IdentityApi extends AbstractApi<IdentityListener> {

    private final ConcurrentMap<Long, SettableFuture<ClientIdentity>> states = new ConcurrentHashMap<>();

    public IdentityApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        super(collar, identityStoreSupplier, sender);
    }

    /**
     * Ask the server to identify the player ID
     * @param playerId to identify
     */
    public Future<ClientIdentity> identify(UUID playerId) {
        SettableFuture<ClientIdentity> future = SettableFuture.create();
        long id = bytesToLong(TokenGenerator.byteToken(4));
        states.put(id, future);
        sender.accept(new GetIdentityRequest(identity(), id, playerId));
        return future;
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.DISCONNECTED) {
            states.clear();
        }
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof GetIdentityResponse) {
            GetIdentityResponse response = (GetIdentityResponse) resp;
            SettableFuture<ClientIdentity> future = states.get(response.id);
            future.set(response.found);
            return true;
        }
        return false;
    }

    private static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    private static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }
}
