package team.catgirl.collar.tests;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.devices.Device;
import team.catgirl.collar.server.services.devices.DeviceService.CreateDeviceResponse;
import team.catgirl.collar.server.services.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.session.SessionManager;
import team.catgirl.collar.tests.junit.CollarClientRule;
import team.catgirl.collar.tests.junit.CollarServerRule;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AuthorizationTest implements CollarListener {

    ServerIdentity identity;
    SessionManager sessions;
    String confirmToken;
    Profile profile;
    AtomicBoolean deviceConfirmed = new AtomicBoolean(false);

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        identity = services.identityStore.getIdentity();
        sessions = services.sessions;
        String username = "user" + System.currentTimeMillis();
        profile = services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                username + "@example.com",
                username,
                "nice profile"
        )).profile;
    });

    @Rule
    public CollarClientRule aliceUser = new CollarClientRule(new CollarConfiguration.Builder().withListener(this));

    @Test
    public void startStopServer() throws InterruptedException {
        waitForCondition("server started", () -> serverRule.isServerStarted());
        waitForCondition("device registered", () -> deviceConfirmed.get());
        waitForCondition("client connected", () -> aliceUser.collar.getState() == Collar.State.CONNECTED);
    }

    @Override
    public void onConfirmDeviceRegistration(Collar collar, String token, String approvalUrl) {
        // We can probably safely assume that this is device 1 for testing purposes
        CreateDeviceResponse resp = new CreateDeviceResponse(new Device(profile.id, 1, "Cool Computer Beep Boop"));
        sessions.onDeviceRegistered(identity, profile.toPublic(), token, resp);
        deviceConfirmed.set(true);
    }

    public void waitForCondition(String name, Supplier<Boolean> condition) throws InterruptedException {
        long future = TimeUnit.MINUTES.toMillis(1) + System.currentTimeMillis();
        while (System.currentTimeMillis() < future) {
            if (condition.get()) {
                return;
            }
            Thread.sleep(200);
        }
        Assert.fail("waitForCondition '" + name + "' failed");
    }
}
