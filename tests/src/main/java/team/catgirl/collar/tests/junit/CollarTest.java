package team.catgirl.collar.tests.junit;

import org.junit.Before;
import org.junit.Rule;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.client.CollarListener;
import team.catgirl.collar.server.Services;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.devices.Device;
import team.catgirl.collar.server.services.devices.DeviceService;
import team.catgirl.collar.server.services.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileService;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public abstract class CollarTest {

    protected Services services;
    protected Profile aliceProfile;
    protected Profile bobProfile;
    protected Profile eveProfile;
    protected final UUID alicePlayerId = UUID.randomUUID();
    protected final UUID bobPlayerId = UUID.randomUUID();
    protected final UUID evePlayerId = UUID.randomUUID();

    private final AtomicInteger devicesConfirmed = new AtomicInteger(0);

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        this.services = services;
        aliceProfile = services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                "alice@example.com",
                "alice",
                "Alice UwU"
        )).profile;
        bobProfile = services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                "bob@example.com",
                "bob",
                "Bob OwO"
        )).profile;
        eveProfile = services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                "eve@example.com",
                "eve",
                "Eve >_>"
        )).profile;
    });

    CollarListener listener = new CollarListener() {
        @Override
        public void onConfirmDeviceRegistration(Collar collar, String token, String approvalUrl) {
            if (aliceProfile != null) {
                approve(token, aliceProfile);
            } else if (bobProfile != null) {
                approve(token, bobProfile);
            } else if (eveProfile != null) {
                approve(token, eveProfile);
            }
        }

        private void approve(String token, Profile profile) {
            DeviceService.CreateDeviceResponse resp = new DeviceService.CreateDeviceResponse(new Device(profile.id, 1, "Cool Computer Beep Boop"));
            services.sessions.onDeviceRegistered(services.identityStore.getIdentity(), profile.toPublic(), token, resp);
            devicesConfirmed.incrementAndGet();
        }
    };

    @Rule
    public CollarClientRule alicePlayer = new CollarClientRule(alicePlayerId, new CollarConfiguration.Builder().withListener(listener));

    @Rule
    public CollarClientRule bobPlayer = new CollarClientRule(bobPlayerId, new CollarConfiguration.Builder().withListener(listener));

    @Rule
    public CollarClientRule evePlayer = new CollarClientRule(evePlayerId, new CollarConfiguration.Builder().withListener(listener));

    @Before
    public void startStopServer() throws InterruptedException {
        waitForCondition("server started", () -> serverRule.isServerStarted());
        waitForCondition("device registered", () -> devicesConfirmed.get() == 3);
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> bobPlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> evePlayer.collar.getState() == Collar.State.CONNECTED);
    }
}
