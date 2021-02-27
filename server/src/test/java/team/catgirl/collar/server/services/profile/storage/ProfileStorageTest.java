package team.catgirl.collar.server.services.profile.storage;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.server.junit.MongoDatabaseTestRule;
import team.catgirl.collar.server.services.profiles.storage.ProfileStorage;

import java.util.List;
import java.util.UUID;

public class ProfileStorageTest {
    @Rule
    public MongoDatabaseTestRule dbRule = new MongoDatabaseTestRule();

    @Test
    public void crud() {
        ProfileStorage storage = new ProfileStorage(dbRule.db);
        byte[] token = TokenGenerator.byteToken(64);
        UUID owner = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();

        storage.store(owner, id1, token, "A");

        List<ProfileStorage.Blob> blobs = storage.find(owner, "A");
        Assert.assertFalse(blobs.isEmpty());

        ProfileStorage.Blob blob = blobs.iterator().next();

        Assert.assertEquals(blob.type, "A");
        Assert.assertEquals(blob.key, id1);
        Assert.assertEquals(blob.owner, owner);
        Assert.assertArrayEquals(blob.data, token);

        storage.delete(owner, id1);
        blobs = storage.find(owner, "A");
        Assert.assertTrue(blobs.isEmpty());
    }
}
