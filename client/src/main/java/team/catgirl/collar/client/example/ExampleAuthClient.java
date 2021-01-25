package team.catgirl.collar.client.example;

import team.catgirl.collar.client.CollarClient;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.client.security.MemoryPlayerIdentityStore;
import team.catgirl.collar.client.security.MemoryServerIdentityStore;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.keyring.MemoryKeyRingManager;

import java.util.UUID;

public class ExampleAuthClient {
    public static void main(String[] args) throws Exception {

        MemoryPlayerIdentityStore playerIdentityStore = new MemoryPlayerIdentityStore();
        MemoryServerIdentityStore server = new MemoryServerIdentityStore();
        MemoryKeyRingManager keyRingManager = new MemoryKeyRingManager();
        CollarClient client = new CollarClient("http://localhost:3000", playerIdentityStore, server, keyRingManager);
        client.connect(UUID.randomUUID(), new CollarListener() {
            @Override
            public void onFingerPrintMismatch(CollarClient client, ServerIdentity serverIdentity) {
                System.out.println("onFingerPrintMismatch");
                client.disconnect();
            }

            @Override
            public void onNewServerIdentity(CollarClient client, ServerIdentity serverIdentity) {
                System.out.println("onNewServerIdentity");
                server.saveIdentity(serverIdentity);
                client.identify();
            }

            @Override
            public void onSessionCreated(CollarClient client) {
                System.out.println("onNewServerIdentity");
            }
        });

        while (true) {
            Thread.sleep(1000);
        }
    }
}
