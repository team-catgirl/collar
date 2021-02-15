package team.catgirl.collar.client.api.messaging;

import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.AbstractApi;
import team.catgirl.collar.client.api.identity.IdentityApi;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MessagingApi extends AbstractApi<MessagingListener> {

//    private final Queue<Message> messages = new LinkedBlockingDeque<>();

    private final Executor executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("Collar Messaging");
        return thread;
    });

    public MessagingApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender, IdentityApi api) {
        super(collar, identityStoreSupplier, sender);
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        return false;
    }

    @Override
    public void onStateChanged(Collar.State state) {

    }

    public void sendMessage(UUID recipientPlayerId, String message) {
        // Find the player client identity by its ID

        Future<ClientIdentity> identityFuture = collar.identities().identify(recipientPlayerId);


//        ClientIdentity identity = collar.identities().identify(recipientPlayerId).get();
//        if (identity == null) {
//            fireListener("", listener -> {
//                listener.onSendMessageToRecipient();
//            });
//        } else {
//            if (!identityStoreSupplier.get().isTrustedIdentity(identity)) {
//                identityStoreSupplier.get().createSendPreKeysRequest()
//            }
//        }


        // check that the client is trusted



        // if not trusted, exchange pre-keys

        // if trusted, send the message
    }

    class PrivateMessageState {
        public final UUID playerId;
        public final ClientIdentity identity;

        public PrivateMessageState(UUID playerId, ClientIdentity identity) {
            this.playerId = playerId;
            this.identity = identity;
        }
    }

    private class Worker implements Runnable {
        @Override
        public void run() {

        }
    }
}
