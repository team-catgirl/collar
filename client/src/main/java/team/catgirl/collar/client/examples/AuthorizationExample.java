package team.catgirl.collar.client.examples;

import team.catgirl.collar.client.CollarClient;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.security.ServerIdentity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class AuthorizationExample {
    public static void main(String[] args) throws Exception {
        Path path = Files.createTempDirectory("example");

        UUID playerId = UUID.randomUUID();
        CollarClient client = new CollarClient("http://localhost:3000", HomeDirectory.fromMinecraftHome(path.toFile(), playerId));
        client.connect(playerId, new CollarListener() {
            @Override
            public void onConnected(CollarClient client, ServerIdentity serverIdentity) {
                System.out.println("Connected to server " + serverIdentity.publicKey.fingerPrint);
            }
        });

        while (client.getState() != CollarClient.ClientState.DISCONNECTED) {
            Thread.sleep(1000);
        }
    }
}
