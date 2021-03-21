package team.catgirl.collar.server.http;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.server.junit.MongoDatabaseTestRule;
import team.catgirl.collar.server.security.hashing.PasswordHashing;
import team.catgirl.collar.server.services.authentication.TokenCrypter;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.api.profiles.ProfileService;
import team.catgirl.collar.server.services.profiles.ProfileServiceServer;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ApiTokenTest {

    @Rule
    public MongoDatabaseTestRule dbRule = new MongoDatabaseTestRule();
    private ProfileService profiles;

    @Before
    public void services() {
        profiles = new ProfileServiceServer(dbRule.db, new PasswordHashing("VSZL*bR8-=r]r5P_"));
    }

    @Test
    public void roundTrip() throws Exception {
        Profile profile = profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest("bob@example.com", "password", "Bob UwU")).profile;
        ApiToken token = new ApiToken(profile.id, new Date().getTime() * TimeUnit.HOURS.toMillis(24), profile.roles);
        TokenCrypter crypter = new TokenCrypter("helloworld");
        String tokenString = token.serialize(crypter);
        ApiToken deserialized = ApiToken.deserialize(crypter, tokenString);
        Assert.assertEquals(deserialized.profileId, token.profileId);
        Assert.assertEquals(deserialized.expiresAt, token.expiresAt);
    }
}
