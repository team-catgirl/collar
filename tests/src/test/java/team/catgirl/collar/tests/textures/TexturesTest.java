package team.catgirl.collar.tests.textures;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.junit.Test;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.GroupType;
import team.catgirl.collar.api.groups.MemberSource;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.api.textures.TextureType;
import team.catgirl.collar.client.api.textures.Texture;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.Services;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.services.textures.TextureService.CreateTextureRequest;
import team.catgirl.collar.tests.junit.CollarAssert;
import team.catgirl.collar.tests.junit.CollarTest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class TexturesTest extends CollarTest {

    UUID groupId = UUID.randomUUID();

    @Override
    protected void withServices(Services services) {
        Profile profile = services.profiles.getProfile(RequestContext.SERVER, ProfileService.GetProfileRequest.byEmail("alice@example.com")).profile;
        ByteSource source = Resources.asByteSource(Resources.getResource("cat.png"));
        try {
            services.textures.createTexture(new RequestContext(profile.id), new CreateTextureRequest(profile.id, null, TextureType.AVATAR, source.read()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        try {
            services.textures.createTexture(new RequestContext(profile.id), new CreateTextureRequest(null, groupId, TextureType.AVATAR, source.read()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void requestPlayerTexture() {
        AtomicReference<Texture> theTexture = new AtomicReference<>();
        alicePlayer.collar.textures().subscribe((collar, texturesApi, texture) -> theTexture.set(texture));

        alicePlayer.collar.textures().requestPlayerTexture(alicePlayer.collar.player(), TextureType.AVATAR);
        CollarAssert.waitForCondition("Receive the requested texture", () -> theTexture.get() != null);

        AtomicReference<Optional<BufferedImage>> imageRef = new AtomicReference<>();
        theTexture.get().loadImage(imageRef::set);

        CollarAssert.waitForCondition("Loaded the texture", () -> imageRef.get().isPresent());
    }

    @Test
    public void requestGroupTexture() {
        AtomicReference<Texture> theTexture = new AtomicReference<>();
        alicePlayer.collar.textures().subscribe((collar, texturesApi, texture) -> theTexture.set(texture));

        Group group = Group.newGroup(groupId, "Cool group", GroupType.GROUP, new MemberSource(new Player(UUID.randomUUID(), new MinecraftPlayer(UUID.randomUUID(), "hypoxel.net")), null), new ArrayList<>());

        alicePlayer.collar.textures().requestGroupTexture(group, TextureType.AVATAR);
        CollarAssert.waitForCondition("Receive the requested texture", () -> theTexture.get() != null);

        AtomicReference<Optional<BufferedImage>> imageRef = new AtomicReference<>();
        theTexture.get().loadImage(imageRef::set);

        CollarAssert.waitForCondition("Loaded the texture", () -> imageRef.get().isPresent());
    }
}
