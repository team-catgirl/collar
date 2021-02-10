package team.catgirl.collar.tests.location;

import org.junit.Assert;
import org.junit.Test;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.groups.GroupInvitation;
import team.catgirl.collar.client.api.groups.GroupsApi;
import team.catgirl.collar.client.api.groups.GroupsListener;
import team.catgirl.collar.client.api.location.LocationApi;
import team.catgirl.collar.client.api.location.LocationListener;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.tests.junit.CollarTest;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class LocationTest extends CollarTest {

    @Test
    public void startAndStopSharing() {
        LocationSharingTestGroupListener aliceGroupListener = new LocationSharingTestGroupListener();
        LocationSharingListener aliceLocationListener = new LocationSharingListener();
        alicePlayer.collar.groups().subscribe(aliceGroupListener);
        alicePlayer.collar.location().subscribe(aliceLocationListener);

        LocationSharingTestGroupListener bobGroupListener = new LocationSharingTestGroupListener();
        LocationSharingListener bobLocationListener = new LocationSharingListener();
        bobPlayer.collar.groups().subscribe(bobGroupListener);
        bobPlayer.collar.location().subscribe(bobLocationListener);

        LocationSharingTestGroupListener eveGroupListener = new LocationSharingTestGroupListener();
        LocationSharingListener eveLocationListener = new LocationSharingListener();
        evePlayer.collar.groups().subscribe(eveGroupListener);
        evePlayer.collar.location().subscribe(eveLocationListener);

        // Put alice and bob in a group
        alicePlayer.collar.groups().create(List.of(bobPlayerId));

        // Put eve in her own group
        evePlayer.collar.groups().create(List.of());

        waitForCondition("Alice joined group", () -> aliceGroupListener.group != null);
        waitForCondition("Bob joined group", () -> bobGroupListener.group != null);
        waitForCondition("Eve joined group", () -> eveGroupListener.group != null);

        // Make sure they aren't in the same group
        Assert.assertNotEquals(aliceGroupListener.group.id, eveGroupListener.group.id);
        Assert.assertNotEquals(bobGroupListener.group.id, eveGroupListener.group.id);
        Assert.assertEquals(bobGroupListener.group.id, aliceGroupListener.group.id);

        Group aliceBobGroup = aliceGroupListener.group;
        alicePlayer.collar.location().startSharingWith(aliceBobGroup);
        bobPlayer.collar.location().startSharingWith(aliceBobGroup);
        evePlayer.collar.location().startSharingWith(eveGroupListener.group);

        Assert.assertTrue(alicePlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertTrue(bobPlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertFalse(evePlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertTrue(evePlayer.collar.location().isSharingWith(eveGroupListener.group));

        waitForCondition("Alice has sent Bob her location", () -> bobPlayer.collar.location().playerLocations().containsKey(alicePlayer.collar.player()), 20, TimeUnit.SECONDS);
        waitForCondition("Bob has sent Alice her location", () -> alicePlayer.collar.location().playerLocations().containsKey(bobPlayer.collar.player()), 20, TimeUnit.SECONDS);
        waitForCondition("Eve doesn't know bob's location", () -> !evePlayer.collar.location().playerLocations().containsKey(bobPlayer.collar.player()), 20, TimeUnit.SECONDS);
        waitForCondition("Eve doesn't know alice's location", () -> !evePlayer.collar.location().playerLocations().containsKey(alicePlayer.collar.player()), 20, TimeUnit.SECONDS);

        Assert.assertNotEquals("last event should be not be unknown location", bobLocationListener.events.getLast().location, Location.UNKNOWN);
        Assert.assertNotEquals("last event should be not be unknown location", aliceLocationListener.events.getLast().location, Location.UNKNOWN);

        alicePlayer.collar.location().stopSharingWith(aliceBobGroup);
        bobPlayer.collar.location().stopSharingWith(aliceBobGroup);
        evePlayer.collar.location().stopSharingWith(eveGroupListener.group);

        Assert.assertFalse(alicePlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertFalse(bobPlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertFalse(evePlayer.collar.location().isSharingWith(aliceBobGroup));
        Assert.assertFalse(evePlayer.collar.location().isSharingWith(eveGroupListener.group));

        waitForCondition("Bob shouldn't have alice's location after she has stopped sharing", () -> !bobPlayer.collar.location().playerLocations().containsKey(alicePlayer.collar.player()), 20, TimeUnit.SECONDS);
        waitForCondition("Alice shouldn't have bob's location after he has stopped sharing", () -> !alicePlayer.collar.location().playerLocations().containsKey(bobPlayer.collar.player()), 20, TimeUnit.SECONDS);

        Assert.assertEquals("last event should be unknown location", bobLocationListener.events.getLast().location, Location.UNKNOWN);
        Assert.assertEquals("last event should be unknown location", aliceLocationListener.events.getLast().location, Location.UNKNOWN);
    }

    public static class Event {
        public final MinecraftPlayer player;
        public final Location location;

        public Event(MinecraftPlayer player, Location location) {
            this.player = player;
            this.location = location;
        }
    }

    public static class LocationSharingListener implements LocationListener {

        public final LinkedList<Event> events = new LinkedList<>();

        @Override
        public void onLocationUpdated(Collar collar, LocationApi locationApi, MinecraftPlayer player, Location location) {
            events.add(new Event(player, location));
        }
    }

    public static class LocationSharingTestGroupListener implements GroupsListener {

        public Group group;

        @Override
        public void onGroupJoined(Collar collar, GroupsApi groupsApi, Group group, MinecraftPlayer player) {
            this.group = group;
        }

        @Override
        public void onGroupInvited(Collar collar, GroupsApi groupsApi, GroupInvitation invitation) {
            groupsApi.accept(invitation);
        }
    }
}
