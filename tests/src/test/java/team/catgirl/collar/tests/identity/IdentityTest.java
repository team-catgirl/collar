package team.catgirl.collar.tests.identity;

import org.junit.Assert;
import org.junit.Test;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.identity.IdentityApi;
import team.catgirl.collar.client.api.identity.IdentityListener;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.tests.junit.CollarTest;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class IdentityTest extends CollarTest {
    @Test
    public void getIdentityForCollarPlayer() throws Exception {
        Optional<ClientIdentity> bobIdentity = alicePlayer.collar.identities().identify(bobPlayerId).get();
        Assert.assertEquals(bobIdentity.get(), bobPlayer.collar.identity());
    }

    @Test
    public void getIdentityForNonCollarPlayer() throws Exception {
        Optional<ClientIdentity> bobIdentity = alicePlayer.collar.identities().identify(UUID.randomUUID()).get();
        Assert.assertTrue(bobIdentity.isEmpty());
    }

    @Test
    public void createTrust() throws Exception {
        IdentityListenerImpl aliceListener = new IdentityListenerImpl();
        alicePlayer.collar.identities().subscribe(aliceListener);

        IdentityListenerImpl bobListener = new IdentityListenerImpl();
        bobPlayer.collar.identities().subscribe(bobListener);

        // Map from bobs player to his identity
        Optional<ClientIdentity> bobIdentity = alicePlayer.collar.identities().identify(bobPlayerId).get();

        // Start setting up bidirectional trust
        Optional<ClientIdentity> trustedIdentity = alicePlayer.collar.identities().createTrust(bobIdentity).get();
        Assert.assertEquals(trustedIdentity.get(), bobIdentity.get());

        // Trust was exchanged
        Assert.assertEquals(bobPlayer.collar.identity(), aliceListener.lastIdentityTrusted);
        Assert.assertEquals(alicePlayer.collar.identity(), bobListener.lastIdentityTrusted);

        // Lets make sure everyone trusts each other
        Assert.assertNotEquals(aliceListener.lastIdentityTrusted, bobListener.lastIdentityTrusted);
        Assert.assertTrue("Bob should be trusted by Alice", aliceListener.identityStore.isTrustedIdentity(bobPlayer.collar.identity()));
        Assert.assertTrue("Alice should be trusted by Bob", bobListener.identityStore.isTrustedIdentity(alicePlayer.collar.identity()));

        // Round trip some bytes so we know we are all setup :)
        byte[] bobBytes = bobListener.identityStore.createCypher().crypt(alicePlayer.collar.identity(), "UwU".getBytes(StandardCharsets.UTF_8));
        String messageFromBob = new String(aliceListener.identityStore.createCypher().decrypt(bobPlayer.collar.identity(), bobBytes), StandardCharsets.UTF_8);
        Assert.assertEquals("UwU", messageFromBob);

        byte[] aliceBytes = aliceListener.identityStore.createCypher().crypt(bobPlayer.collar.identity(), "OwO".getBytes(StandardCharsets.UTF_8));
        String messageFromAlice = new String(bobListener.identityStore.createCypher().decrypt(alicePlayer.collar.identity(), aliceBytes), StandardCharsets.UTF_8);
        Assert.assertEquals("OwO", messageFromAlice);
    }

    public static final class IdentityListenerImpl implements IdentityListener {

        public ClientIdentity lastIdentityTrusted;
        public ClientIdentityStore identityStore;

        @Override
        public void onIdentityTrusted(Collar collar, IdentityApi api, ClientIdentityStore identityStore, ClientIdentity identity) {
            this.lastIdentityTrusted = identity;
            this.identityStore = identityStore;
            Assert.assertTrue(identity + " was not trusted in " + collar.identity() + "'s store", identityStore.isTrustedIdentity(identity));
        }
    }
}
