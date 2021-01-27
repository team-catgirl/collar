package team.catgirl.collar.client.examples;

import team.catgirl.collar.client.CollarClient;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.messages.ServerMessage;
import team.catgirl.collar.models.Position;
import team.catgirl.collar.security.ServerIdentity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

            @Override
            public void onIdentityCreated(CollarClient client, ServerMessage.CreateIdentityResponse resp) {
//                client.identify()
            }

            @Override
            public void onSessionCreated(CollarClient client) {
                System.out.println("onSessionCreated");
                try {
                    ArrayList<UUID> players = new ArrayList<>();
                    players.add(UUID.randomUUID());
                    client.createGroup(players, new Position(1d, 1d, 1d, 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onGroupCreated(CollarClient client, ServerMessage.CreateGroupResponse resp) {
                System.out.println("onGroupCreated");
                try {
                    client.updatePosition(new Position(1d, 1d, 1d, 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        while (client.getState() != CollarClient.ClientState.DISCONNECTED) {
            Thread.sleep(1000);
        }
    }
}
