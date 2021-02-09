package team.catgirl.collar.tests.server;

import org.junit.Test;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.tests.junit.CollarTest;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class AuthorizationTest extends CollarTest {
    @Test
    public void connect() throws InterruptedException {
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> bobPlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> evePlayer.collar.getState() == Collar.State.CONNECTED);
    }
}
