package team.catgirl.collar.tests.server;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import team.catgirl.collar.api.location.Dimension;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.client.utils.Http;
import team.catgirl.collar.security.mojang.MinecraftSession;
import team.catgirl.collar.security.mojang.ServerAuthentication.HasJoinedResponse;
import team.catgirl.collar.security.mojang.ServerAuthentication.JoinRequest;
import team.catgirl.collar.server.Services;
import team.catgirl.collar.server.configuration.Configuration;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.server.mongo.Mongo;
import team.catgirl.collar.server.security.mojang.MojangMinecraftSessionVerifier;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileServiceServer;
import team.catgirl.collar.tests.junit.CollarClientRule;
import team.catgirl.collar.tests.junit.CollarServerRule;
import team.catgirl.collar.tests.junit.CollarTest;
import team.catgirl.collar.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static spark.Spark.get;
import static spark.Spark.post;
import static team.catgirl.collar.tests.junit.CollarAssert.waitForCondition;

@Ignore("seems to have broken but we are going to fix this later")
public class YggdrasilTest {

    private final AtomicReference<Services> services = new AtomicReference<>();
    private final AtomicReference<Profile> aliceProfile = new AtomicReference<>();
    private final AtomicInteger devicesConfirmed = new AtomicInteger();
    private final UUID alicePlayerId = UUID.randomUUID();

    private final ConcurrentHashMap<String, JoinRequest> joinRequestMap = new ConcurrentHashMap<>();

    @Rule
    public CollarServerRule serverRule = new CollarServerRule(services -> {
        this.services.set(services);
        aliceProfile.set(services.profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest(
                "alice@example.com",
                "alice",
                "Alice"
        )).profile);

        post("/test/session/minecraft/join", (request, response) -> {
            String body = request.body();
            if (body == null) throw new IllegalStateException("no body");
            JoinRequest joinRequest = Utils.jsonMapper().readValue(body, JoinRequest.class);
            joinRequestMap.put(joinRequest.selectedProfile, joinRequest);
            response.status(204);
            return null;
        });
        
        get("/test/session/minecraft/hasJoined", (request, response) -> {
            String username = request.queryParams("username");
            if ((username == null || !username.equals("alice"))) {
                response.status(401);
                return null;
            } else {
                response.status(200);
                return new HasJoinedResponse(alicePlayerId.toString().replace("-", ""), "alice");
            }
        });
    }, Configuration.testConfiguration(Mongo.getTestingDatabase(), new MojangMinecraftSessionVerifier(Http.client(), "http://localhost:3001/test/")));

    @Rule
    public CollarClientRule aliceClient = new CollarClientRule(alicePlayerId, new CollarConfiguration.Builder()
            .withListener(new CollarTest.ApprovingListener(aliceProfile, services, devicesConfirmed))
            .withPlayerLocation(() -> new Location(Utils.secureRandom().nextDouble(), Utils.secureRandom().nextDouble(), Utils.secureRandom().nextDouble(), Dimension.OVERWORLD))
            .withEntitiesSupplier(HashSet::new)
            .withYggdrasilBaseUrl("http://localhost:3001/test/"), MinecraftSession.mojang(alicePlayerId, "alice", 1, "2b2t.org", "mytoken"));

    @Test
    public void authWithYggdrasil() {
        waitForCondition("server started", () -> serverRule.isServerStarted());
        waitForCondition("device registered", () -> devicesConfirmed.get() == 1);
        waitForCondition("alice connected", () -> aliceClient.collar.getState() == Collar.State.CONNECTED, 25, TimeUnit.SECONDS);
    }
}
