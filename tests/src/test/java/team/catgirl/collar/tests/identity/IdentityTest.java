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
    public void getIdentityForCollarPlayer() throws Exception {
        ClientIdentity bobIdentity = alicePlayer.collar.identities().identify(bobPlayerId).get();
        Assert.assertEquals(bobIdentity, bobPlayer.collar.identity());
    }

    @Test
    public void getIdentityForNonCollarPlayer() throws Exception {
        ClientIdentity bobIdentity = alicePlayer.collar.identities().identify(UUID.randomUUID()).get();
        Assert.assertNull(bobIdentity);
    }
}
