package team.catgirl.collar.examples;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.location.Dimension;
import team.catgirl.collar.api.location.Position;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.client.api.groups.GroupInvitation;
import team.catgirl.collar.client.api.groups.GroupListener;
import team.catgirl.collar.client.api.groups.GroupsFeature;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class GroupsExample {
    public static void main(String[] args) throws Exception {
        GroupListener listener = new GroupListener() {
            @Override
            public void onGroupCreated(Collar collar, GroupsFeature feature, Group group) {
                System.out.println("Group created!! " + group.id);
            }

            @Override
            public void onGroupJoined(Collar collar, GroupsFeature feature, Group group) {
            }

            @Override
            public void onGroupLeft(Collar collar, GroupsFeature feature, Group group) {
            }

            @Override
            public void onGroupsUpdated(Collar collar, GroupsFeature feature, Group group) {
            }

            @Override
            public void onGroupMemberInvitationsSent(Collar collar, GroupsFeature feature, Group group) {
            }

            @Override
            public void onGroupInvited(Collar collar, GroupsFeature feature, GroupInvitation invitation) {
            }
        };

        CollarListener collarListener = new CollarListener() {

            @Override
            public void onConfirmDeviceRegistration(Collar collar, String approvalUrl) {
                System.out.println("Please follow the following link to confirm: " + approvalUrl);
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

            @Override
            public void onStateChanged(Collar collar, Collar.State state) {
                switch (state) {
                    case CONNECTED:
                        collar.groups().subscribe(listener);
                        collar.groups().create(new ArrayList<>());
                        break;
                    case DISCONNECTED:
                        collar.groups().unsubscribe(listener);
                        break;
                }
            }
        };

        CollarConfiguration configuration = new CollarConfiguration.Builder()
                .withCollarServer("http://localhost:3000/")
                .withHomeDirectory(new File("target"))
                .withMojangAuthentication(() -> MinecraftSession.noJang(UUID.randomUUID(), "smp.catgirl.team"))
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
