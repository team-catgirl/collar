package team.catgirl.collar.client.examples;

import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.client.State;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class AuthorizationExample {
    public static void main(String[] args) throws Exception {
        Path path = Files.createTempDirectory("example");
        File file = path.toFile();
        UUID playerId = UUID.randomUUID();
        Collar collar = Collar.create(playerId, "http://localhost:3000/", file, new CollarListener() {
            @Override
            public void onConfirmDeviceRegistration(Collar collar, RegisterDeviceResponse resp) {
                System.out.println(resp.approvalUrl);
            }
        });

        collar.connect();

        while (collar.getState() != State.DISCONNECTED) {
            Thread.sleep(1000);
        }
    }
}
