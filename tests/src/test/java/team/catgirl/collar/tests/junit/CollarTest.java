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
import java.util.concurrent.atomic.AtomicReference;

import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

public abstract class CollarTest {

    private final AtomicReference<Services> services = new AtomicReference<>();
    private final AtomicReference<Profile> aliceProfile = new AtomicReference<>();
    private final AtomicReference<Profile> bobProfile = new AtomicReference<>();
    private final AtomicReference<Profile> eveProfile = new AtomicReference<>();
    protected final UUID alicePlayerId = UUID.randomUUID();
    protected final UUID bobPlayerId = UUID.randomUUID();
    protected final UUID evePlayerId = UUID.randomUUID();

    private final AtomicInteger devicesConfirmed = new AtomicInteger(0);

    protected Services getServices() {
        return services.get();
    }

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        this.services.set(services);
         aliceProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                "alice@example.com",
                "alice",
                "Alice UwU"
        )).profile);
        bobProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                "bob@example.com",
                "bob",
                "Bob OwO"
        )).profile);
        eveProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileService.CreateProfileRequest(
                "eve@example.com",
                "eve",
                "Eve >_>"
        )).profile);
    });

    @Rule
    public CollarClientRule alicePlayer = new CollarClientRule(alicePlayerId, new CollarConfiguration.Builder().withListener(new ApprovingListener(aliceProfile, services, devicesConfirmed)));

    @Rule
    public CollarClientRule bobPlayer = new CollarClientRule(bobPlayerId, new CollarConfiguration.Builder().withListener(new ApprovingListener(bobProfile, services, devicesConfirmed)));

    @Rule
    public CollarClientRule evePlayer = new CollarClientRule(evePlayerId, new CollarConfiguration.Builder().withListener(new ApprovingListener(eveProfile, services, devicesConfirmed)));

    @Before
    public void waitForClientsToConnect() throws InterruptedException {
        waitForCondition("server started", () -> serverRule.isServerStarted());
        waitForCondition("device registered", () -> devicesConfirmed.get() == 3);
        waitForCondition("client connected", () -> alicePlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> bobPlayer.collar.getState() == Collar.State.CONNECTED);
        waitForCondition("client connected", () -> evePlayer.collar.getState() == Collar.State.CONNECTED);

        System.out.println("Alice is identity " + alicePlayer.collar.identity());
        System.out.println("Bob is identity " + bobPlayer.collar.identity());
        System.out.println("Eve is identity " + evePlayer.collar.identity());

        System.out.println("Alice is player " + alicePlayer.collar.player());
        System.out.println("Bob is player " + bobPlayer.collar.player());
        System.out.println("Eve is player " + evePlayer.collar.player());
    }

    /** Approves the device on initial startup of client **/
    public static class ApprovingListener implements CollarListener {
        private final AtomicReference<Profile> profile;
        private final AtomicReference<Services> services;
        private final AtomicInteger devicesConfirmed;

        public ApprovingListener(AtomicReference<Profile> profile, AtomicReference<Services> services, AtomicInteger devicesConfirmed) {
            this.profile = profile;
            this.services = services;
            this.devicesConfirmed = devicesConfirmed;
        }

        @Override
        public void onConfirmDeviceRegistration(Collar collar, String token, String approvalUrl) {
            Profile profile = this.profile.get();
            Services services = this.services.get();
            DeviceService.CreateDeviceResponse resp = new DeviceService.CreateDeviceResponse(new Device(profile.id, 1, "Cool Computer Beep Boop"));
            services.sessions.onDeviceRegistered(services.identityStore.getIdentity(), profile.toPublic(), token, resp);
            devicesConfirmed.incrementAndGet();
        }
    }
}
