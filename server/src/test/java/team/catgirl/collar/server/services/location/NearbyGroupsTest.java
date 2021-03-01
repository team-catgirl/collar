package team.catgirl.collar.server.services.location;

import org.junit.Assert;
import org.junit.Test;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.Set;
import java.util.UUID;

public class NearbyGroupsTest {
    @Test
    public void calculateGroup() {
        Player alice = new Player(UUID.randomUUID(), new MinecraftPlayer(UUID.randomUUID(), "cute"));
        Player bob = new Player(UUID.randomUUID(), new MinecraftPlayer(UUID.randomUUID(), "cute"));
        Player eve = new Player(UUID.randomUUID(), new MinecraftPlayer(UUID.randomUUID(), "cute"));

        NearbyGroups groups = new NearbyGroups();
        NearbyGroups.Result result = groups.updateNearbyGroups(alice, Set.of("alice", "bob"));
        Assert.assertTrue(result.add.isEmpty());
        Assert.assertTrue(result.remove.isEmpty());

        result = groups.updateNearbyGroups(bob, Set.of("alice", "bob"));
        Assert.assertFalse(result.add.isEmpty());
        Assert.assertTrue(result.remove.isEmpty());

        NearbyGroup group = result.add.values().iterator().next();
        Assert.assertTrue(group.players.contains(alice));
        Assert.assertTrue(group.players.contains(bob));

        result = groups.updateNearbyGroups(eve, Set.of("eve", "alice"));
        Assert.assertTrue(result.add.isEmpty());
        Assert.assertTrue(result.remove.isEmpty());

        result = groups.updateNearbyGroups(bob, Set.of("alice", "bob"));
        Assert.assertTrue(result.add.isEmpty());
        Assert.assertTrue(result.remove.isEmpty());

        result = groups.updateNearbyGroups(alice, Set.of("alice", "eve"));
        Assert.assertFalse(result.add.isEmpty());

        group = result.add.values().iterator().next();
        Assert.assertEquals(2, group.players.size());
        Assert.assertTrue(group.players.contains(alice));
        Assert.assertTrue(group.players.contains(eve));

        Assert.assertFalse(result.remove.isEmpty());
        group = result.remove.values().iterator().next();
        Assert.assertEquals(2, group.players.size());
        Assert.assertTrue(group.players.contains(alice));
        Assert.assertTrue(group.players.contains(bob));
    }
}
