package team.catgirl.collar.api.profiles;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.http.RequestContext;

import java.util.UUID;

public interface ProfileService {
    CreateProfileResponse createProfile(RequestContext context, CreateProfileRequest req);

    GetProfileResponse getProfile(RequestContext context, GetProfileRequest req);

    UpdateProfileResponse updateProfile(RequestContext context, UpdateProfileRequest req);

    class CreateProfileRequest {
        public final String email;
        public final String password;
        public final String name;

        public CreateProfileRequest(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }
    }

    class CreateProfileResponse {
        public final Profile profile;

        public CreateProfileResponse(Profile profile) {
            this.profile = profile;
        }
    }

    class GetProfileRequest {
        public final UUID byId;
        public final String byEmail;

        public GetProfileRequest(UUID byId, String byEmail) {
            this.byId = byId;
            this.byEmail = byEmail;
        }

        public static GetProfileRequest byId(UUID uuid) {
            return new GetProfileRequest(uuid, null);
        }

        public static GetProfileRequest byEmail(String email) {
            return new GetProfileRequest(null, email);
        }
    }

    class GetProfileResponse {
        public final Profile profile;

        public GetProfileResponse(Profile profile) {
            this.profile = profile;
        }
    }

    final class UpdateProfileRequest {
        @JsonProperty("profile")
        public final UUID profile;
        @JsonProperty("emailVerified")
        public final Boolean emailVerified;
        @JsonProperty("hashedPassword")
        public final String hashedPassword;
        @JsonProperty("privateIdentityToken")
        public final byte[] privateIdentityToken;
        public final UUID addMinecraftAccount;
        @JsonProperty("cape")
        public final TexturePreference cape;

        public UpdateProfileRequest(@JsonProperty("profile") UUID profile,
                                    @JsonProperty("emailVerified") Boolean emailVerified,
                                    @JsonProperty("hashedPassword") String hashedPassword,
                                    @JsonProperty("privateIdentityToken") byte[] privateIdentityToken,
                                    @JsonProperty("addMinecraftAccount") UUID addMinecraftAccount,
                                    @JsonProperty("cape") TexturePreference cape) {
            this.profile = profile;
            this.emailVerified = emailVerified;
            this.hashedPassword = hashedPassword;
            this.privateIdentityToken = privateIdentityToken;
            this.addMinecraftAccount = addMinecraftAccount;
            this.cape = cape;
        }

        public static UpdateProfileRequest emailVerified(UUID profile) {
            return new UpdateProfileRequest(profile, true, null, null, null, null);
        }

        public static UpdateProfileRequest hashedPassword(UUID profile, String newPassword) {
            return new UpdateProfileRequest(profile, null, newPassword, null, null, null);
        }

        public static UpdateProfileRequest privateIdentityToken(UUID profile, byte[] privateIdentityToken) {
            return new UpdateProfileRequest(profile, null, null, privateIdentityToken, null, null);
        }

        public static UpdateProfileRequest capeTexturePreference(UUID profile, TexturePreference capeTexture) {
            return new UpdateProfileRequest(profile, null, null, null, null, capeTexture);
        }

        public static UpdateProfileRequest addMinecraftAccount(UUID profile, UUID minecraftAccount) {
            return new UpdateProfileRequest(profile, null, null, null, minecraftAccount, null);
        }
    }

    final class UpdateProfileResponse {
        @JsonProperty("profile")
        public final Profile profile;

        public UpdateProfileResponse(@JsonProperty("profile") Profile profile) {
            this.profile = profile;
        }
    }
}
