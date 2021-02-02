package team.catgirl.collar.examples;

import com.google.common.io.Files;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.io.File;
import java.io.IOException;

public class AuthorizationExample {
    public static void main(String[] args) throws Exception {
        String username = args[0];
        String password = args[1];
        File file = new File("target");
        MinecraftSession minecraftSession = MinecraftSession.from(username, password, "smp.catgirl.team");
        Collar collar = Collar.create(minecraftSession, "http://localhost:3000/", file, new CollarListener() {
            @Override
            public void onConfirmDeviceRegistration(Collar collar, String approvalUrl) {
                System.out.println("Please follow the following link to confirm: " + approvalUrl);
            }

            @Override
            public void onClientUntrusted(Collar collar, ClientIdentityStore store) {
                System.out.println("Client is untrusted - resetting store?");
            }

            @Override
            public void onMinecraftAccountVerificationFailed(Collar collar, MinecraftSession session) {
                collar.disconnect();
            }
        });

        collar.connect();

        while (collar.getState() != Collar.State.DISCONNECTED) {
            Thread.sleep(1000);
        }
    }
}
