package team.catgirl.collar.client.examples;

import team.catgirl.collar.client.Collar;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class AuthorizationExample {
    public static void main(String[] args) throws Exception {
        Path path = Files.createTempDirectory("example");
        File file = path.toFile();
        UUID playerId = UUID.randomUUID();
        Collar collar = Collar.create(playerId, "http://localhost:3000/", file);

        while (true) {
//        while (client.getState() != CollarClient.ClientState.DISCONNECTED) {
            Thread.sleep(1000);
        }
    }
}
