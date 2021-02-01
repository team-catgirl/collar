package team.catgirl.collar.server.services.authentication;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.HttpException.BadRequestException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.api.http.HttpException.UnauthorisedException;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.server.http.AuthToken;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.security.hashing.PasswordHashing;
import team.catgirl.collar.server.services.profiles.Profile;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.services.profiles.ProfileService.CreateProfileRequest;
import team.catgirl.collar.server.services.profiles.ProfileService.GetProfileRequest;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AuthenticationService {

    private final ProfileService profiles;
    private final PasswordHashing passwordHashing;
    private final TokenCrypter tokenCrypter;


    public AuthenticationService(ProfileService profiles, PasswordHashing passwordHashing, TokenCrypter tokenCrypter) {
        this.profiles = profiles;
        this.passwordHashing = passwordHashing;
        this.tokenCrypter = tokenCrypter;
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
        } catch (HttpException.NotFoundException e) {
            Profile profile = profiles.createProfile(context, new CreateProfileRequest(req.email.toLowerCase(), req.password, req.name)).profile;
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
        } catch (HttpException.NotFoundException e) {
            // Do not leak existence of account by letting NotFoundException propagate
            throw new UnauthorisedException("login failed");
        }
        BCrypt.Result result = passwordHashing.verifyer().verify(req.password.toCharArray(), Objects.requireNonNull(profile.hashedPassword).toCharArray());
        if (result.verified) {
            return new LoginResponse(profile, tokenFrom(profile));
        } else {
            throw new UnauthorisedException("login failed");
        }
    }

    private String tokenFrom(Profile profile) {
        AuthToken authToken = new AuthToken(profile.id, new Date().getTime() * TimeUnit.HOURS.toMillis(24));
        String token;
        try {
            token = authToken.serialize(tokenCrypter);
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
}
