package team.catgirl.collar.examples;

import com.google.common.io.Files;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.io.File;
import java.io.IOException;

public class AuthorizationExample {
    public static void main(String[] args) throws Exception {
        String username = args[1];
        String password = args[2];
        File file = Files.createTempDir();
        MinecraftSession minecraftSession = MinecraftSession.from(username, password, "smp.catgirl.team");
        Collar collar = Collar.create(minecraftSession, "http://localhost:3000/", file, new CollarListener() {
            @Override
            public void onConfirmDeviceRegistration(Collar collar, RegisterDeviceResponse resp) {
                System.out.println("Please follow the following link to confirm: " + resp.approvalUrl);
            }

            @Override
            public void onClientUntrusted(Collar collar, ClientIdentityStore store) {
                try {
                    System.out.println("Client is untrusted - resetting store");
                    store.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        collar.connect();

        while (collar.getState() != Collar.State.DISCONNECTED) {
            Thread.sleep(1000);
        }
    }
}
