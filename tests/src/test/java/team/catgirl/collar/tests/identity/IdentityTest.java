package team.catgirl.collar.tests.identity;

import org.junit.Assert;
import org.junit.Test;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.identity.IdentityApi;
import team.catgirl.collar.client.api.identity.IdentityListener;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.tests.junit.CollarAssert;
import team.catgirl.collar.tests.junit.CollarTest;

import java.util.UUID;

public class IdentityTest extends CollarTest {
    @Test
    public void canGetClientIdentity() throws Exception {
//        IdentityListenerImpl aliceListener = new IdentityListenerImpl();
//        alicePlayer.collar.identities().subscribe(aliceListener);

        ClientIdentity clientIdentity = alicePlayer.collar.identities().identify(bobPlayerId).get();
        Assert.assertEquals(clientIdentity, bobPlayer.collar.identity());
    }

//    private static class IdentityListenerImpl implements IdentityListener {
//
//        public ClientIdentity identity;
//        public UUID player;
//
//        @Override
//        public void onPlayerIdentified(Collar collar, IdentityApi identityApi, ClientIdentityStore identityStore, UUID playerId, ClientIdentity identity) {
//            this.identity = identity;
//            this.player = playerId;
//        }
//    }
}
