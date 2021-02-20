package team.catgirl.collar.server.services.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.HttpException.*;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.server.http.AppUrlProvider;
import team.catgirl.collar.server.http.ApiToken;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.mail.Email;
import team.catgirl.collar.server.security.hashing.PasswordHashing;
import team.catgirl.collar.server.services.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.services.profiles.ProfileService.CreateProfileRequest;
import team.catgirl.collar.server.services.profiles.ProfileService.GetProfileRequest;
import team.catgirl.collar.server.services.profiles.ProfileService.UpdateProfileRequest;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthenticationService {

    private final static Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());

    private final ProfileService profiles;
    private final PasswordHashing passwordHashing;
    private final TokenCrypter tokenCrypter;
    private final Email email;
    private final AppUrlProvider urlProvider;


    public AuthenticationService(ProfileService profiles, PasswordHashing passwordHashing, TokenCrypter tokenCrypter, Email email, AppUrlProvider urlProvider) {
        this.profiles = profiles;
        this.passwordHashing = passwordHashing;
        this.tokenCrypter = tokenCrypter;
        this.email = email;
        this.urlProvider = urlProvider;
    }

    public CreateAccountResponse createAccount(RequestContext context, CreateAccountRequest req) {
        context.assertAnonymous();
        if (req.name == null) {
            throw new BadRequestException("name missing");
        }
        if (req.email == null){
            throw new BadRequestException("email missing");
        }
        if (req.password == null) {
            throw new BadRequestException("password missing");
        }
        if (req.confirmPassword == null) {
            throw new BadRequestException("confirmPassword missing");
        }
        if (!req.password.equals(req.confirmPassword)) {
            throw new BadRequestException("password mismatch");
        }
        try {
            profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byEmail(req.email));
            throw new HttpException.ConflictException("user already exists");
        } catch (NotFoundException e) {
            Profile profile = profiles.createProfile(context, new CreateProfileRequest(req.email.toLowerCase(), req.password, req.name)).profile;
            VerificationToken apiToken = new VerificationToken(profile.id, new Date().getTime() + TimeUnit.HOURS.toMillis(24));
            String url;
            try {
                url = urlProvider.emailVerificationUrl(apiToken.serialize(tokenCrypter));
            } catch (IOException ioException) {
                throw new ServerErrorException("token could not be serialized", e);
            }
            email.send(profile, "Verify your new Collar account", "verify-account", Map.of("verificationUrl", url));
            return new CreateAccountResponse(profile.toPublic(), tokenFrom(profile));
        }
    }

    public LoginResponse login(RequestContext context, LoginRequest req) {
        context.assertAnonymous();
        if (req.email == null) {
            throw new BadRequestException("email missing");
        }
        if (req.password == null) {
            throw new BadRequestException("password missing");
        }
        Profile profile;
        try {
            profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byEmail(req.email)).profile;
        } catch (NotFoundException e) {
            // Do not leak existence of account by letting NotFoundException propagate
            throw new UnauthorisedException("login failed");
        }
        if (passwordHashing.verify(req.password.toCharArray(), Objects.requireNonNull(profile.hashedPassword).toCharArray())) {
            return new LoginResponse(profile, tokenFrom(profile));
        } else {
            throw new UnauthorisedException("login failed");
        }
    }

    public VerifyAccountResponse verify(RequestContext context, VerifyAccountRequest req) {
        context.assertAnonymous();
        return VerificationToken.from(tokenCrypter, req.token).map(verificationToken -> {
            Profile profile = profiles.updateProfile(RequestContext.from(verificationToken.profileId), UpdateProfileRequest.emailVerified(verificationToken.profileId)).profile;
            if (!profile.emailVerified) {
                throw new UnauthorisedException("Verify your email address first before logging in");
            }
            LOGGER.log(Level.INFO, "Verified account for " + profile.id);
            return new VerifyAccountResponse(urlProvider.homeUrl());
        }).orElseThrow(() -> new UnauthorisedException("bad or missing token"));
    }

    public RequestPasswordResponse requestPasswordReset(RequestContext context, RequestPasswordResetRequest req) {
        context.assertAnonymous();
        Profile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byEmail(req.email)).profile;
        VerificationToken apiToken = new VerificationToken(profile.id, new Date().getTime() + TimeUnit.HOURS.toMillis(24));
        String url;
        try {
            url = urlProvider.resetPassword(apiToken.serialize(tokenCrypter));
        } catch (IOException e) {
            throw new ServerErrorException("token could not be serialized", e);
        }
        email.send(profile, "Verify your new Collar account", "verify-account", Map.of("resetPasswordUrl", url));
        return new RequestPasswordResponse();
    }

    public ResetPasswordResponse resetPassword(RequestContext context, ResetPasswordRequest req) {
        context.assertAnonymous();
        VerificationToken token = VerificationToken.from(tokenCrypter, req.token).orElseThrow(() -> new BadRequestException("token missing"));
        Profile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(token.profileId)).profile;
        String oldPassword = passwordHashing.hash(req.oldPassword);
        if (!Objects.equals(profile.hashedPassword, oldPassword)) {
            throw new ForbiddenException("Incorrect password");
        }
        String newPassword = passwordHashing.hash(req.newPassword);
        profile = profiles.updateProfile(RequestContext.SERVER, UpdateProfileRequest.hashedPassword(token.profileId, newPassword)).profile;
        return new ResetPasswordResponse(profile.toPublic());
    }

    private String tokenFrom(Profile profile) {
        ApiToken apiToken = new ApiToken(profile.id, new Date().getTime() * TimeUnit.HOURS.toMillis(24));
        String token;
        try {
            token = apiToken.serialize(tokenCrypter);
        } catch (IOException e) {
            throw new ServerErrorException("token generation error", e);
        }
        return token;
    }

    public static class CreateAccountRequest {
        @JsonProperty("email")
        public final String email;
        @JsonProperty("name")
        public final String name;
        @JsonProperty("password")
        public final String password;
        @JsonProperty("confirmPassword")
        public final String confirmPassword;

        public CreateAccountRequest(
                @JsonProperty("email") String email,
                @JsonProperty("name") String name,
                @JsonProperty("password") String password,
                @JsonProperty("confirmPassword") String confirmPassword) {
            this.email = email;
            this.name = name;
            this.password = password;
            this.confirmPassword = confirmPassword;
        }
    }

    public static class CreateAccountResponse {
        @JsonProperty("profile")
        public final PublicProfile profile;
        @JsonProperty("token")
        public final String token;

        public CreateAccountResponse(@JsonProperty("profile") PublicProfile profile, @JsonProperty("token") String token) {
            this.profile = profile;
            this.token = token;
        }
    }

    public static class LoginRequest {
        @JsonProperty("email")
        public final String email;
        @JsonProperty("password")
        public final String password;

        public LoginRequest(@JsonProperty("email") String email, @JsonProperty("password") String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class LoginResponse {
        @JsonProperty("profile")
        public final Profile profile;
        @JsonProperty("token")
        public final String token;

        public LoginResponse(@JsonProperty("profile") Profile profile, @JsonProperty("token") String token) {
            this.profile = profile;
            this.token = token;
        }
    }

    public static class VerifyAccountRequest {
        @JsonProperty("token")
        public final String token;

        public VerifyAccountRequest(@JsonProperty("token") String token) {
            this.token = token;
        }
    }

    public static class VerifyAccountResponse {
        @JsonProperty("redirectUrl")
        public final String redirectUrl;

        public VerifyAccountResponse(@JsonProperty("redirectUrl") String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }
    }

    public static class RequestPasswordResetRequest {
        public final String email;

        public RequestPasswordResetRequest(String email) {
            this.email = email;
        }
    }

    public static class RequestPasswordResponse {}

    public static class ResetPasswordRequest {
        public final String token;
        public final String oldPassword;
        public final String newPassword;

        public ResetPasswordRequest(String token, String oldPassword, String newPassword) {
            this.token = token;
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }
    }

    public static class ResetPasswordResponse {
        @JsonProperty("profile")
        private final PublicProfile profile;

        public ResetPasswordResponse(PublicProfile profile) {

            this.profile = profile;
        }
    }
}
