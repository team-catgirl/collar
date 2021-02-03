package team.catgirl.collar.examples;

import com.google.common.io.Files;
import team.catgirl.collar.api.location.Dimension;
import team.catgirl.collar.api.location.Position;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.io.File;
import java.io.IOException;

public class AuthorizationExample {
    public static void main(String[] args) throws Exception {
        String username = args[0];
        String password = args[1];

        CollarListener collarListener = new CollarListener() {
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
        };

        CollarConfiguration configuration = new CollarConfiguration.Builder()
                .withCollarServer("http://localhost:3000/")
                .withHomeDirectory(new File("target"))
                .withMojangAuthentication(() -> MinecraftSession.from(username, password, "smp.catgirl.team"))
                .withPlayerPosition(() -> new Position(1d, 1d, 1d, Dimension.OVERWORLD))
                .withListener(collarListener)
                .build();
        Collar collar = Collar.create(configuration);

        collar.connect();

        while (collar.getState() != Collar.State.DISCONNECTED) {
            Thread.sleep(1000);
        }
    }
}
