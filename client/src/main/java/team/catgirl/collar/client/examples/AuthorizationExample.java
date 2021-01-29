package team.catgirl.collar.client.examples;

import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.client.HomeDirectory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class AuthorizationExample {
    public static void main(String[] args) throws Exception {
        Path path = Files.createTempDirectory("example");
        File file = path.toFile();
        UUID playerId = UUID.randomUUID();
        HomeDirectory.from(path.toFile(), playerId)
        String baseUrl = "http://localhost:3000";
//        CollarClient client = new CollarClient("http://localhost:3000", HomeDirectory.fromMinecraftHome(path.toFile(), playerId));

        CollarListener listener;

        Collar collar = Collar.create(playerId, baseUrl, file);

        collar.connect()

        while (true) {
//        while (client.getState() != CollarClient.ClientState.DISCONNECTED) {
            Thread.sleep(1000);
        }
    }
}
