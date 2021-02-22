package team.catgirl.collar.tests.server;

import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.server.Services;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.tests.junit.CollarClientRule;
import team.catgirl.collar.tests.junit.CollarServerRule;
import team.catgirl.collar.tests.junit.CollarTest;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public class HandshakeTest {

    private final AtomicReference<Services> services = new AtomicReference<>();
    private final AtomicReference<Profile> aliceProfile = new AtomicReference<>();
    private final AtomicInteger devicesConfirmed = new AtomicInteger(0);
    UUID alicePlayerId = UUID.randomUUID();

    private final File tempDir = Files.createTempDir();

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        this.services.set(services);
        aliceProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                "alice@example.com",
                "alice",
                "Alice UwU"
        )).profile);
    });

    @Rule
    public CollarClientRule alicePlayer = new CollarClientRule(alicePlayerId, new CollarConfiguration.Builder()
            .withListener(new CollarTest.ApprovingListener(aliceProfile, services, devicesConfirmed))
            .withHomeDirectory(tempDir)
            .withEntitiesSupplier(Set::of)
    );

    @Test
    public void initialRegistrationAndLoginDoesNotExplode() throws Exception {
        // Wait for the server to start and for alice to connect
        waitForCondition("server started", () -> serverRule.isServerStarted());
        waitForCondition("device registered", () -> devicesConfirmed.get() == 1);
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);

        // Disconnect alice
        alicePlayer.collar.disconnect();

        waitForCondition("client disconnected", () -> alicePlayer.collar.getState() == Collar.State.DISCONNECTED);

        devicesConfirmed.set(0);

        alicePlayer.collar.connect();

        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("device registered", () -> devicesConfirmed.get() == 0);
    }
}
