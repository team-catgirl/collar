package team.catgirl.collar.server.services.authentication;

import team.catgirl.collar.api.authentication.AuthenticationService;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.HttpException.*;
import team.catgirl.collar.server.http.ApiToken;
import team.catgirl.collar.server.http.AppUrlProvider;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.server.mail.Email;
import team.catgirl.collar.server.security.hashing.PasswordHashing;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.api.profiles.ProfileService;
import team.catgirl.collar.api.profiles.ProfileService.CreateProfileRequest;
import team.catgirl.collar.api.profiles.ProfileService.GetProfileRequest;
import team.catgirl.collar.api.profiles.ProfileService.UpdateProfileRequest;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerAuthenticationService implements AuthenticationService {

    private final static Logger LOGGER = Logger.getLogger(ServerAuthenticationService.class.getName());

    private final ProfileService profiles;
    private final PasswordHashing passwordHashing;
    private final TokenCrypter tokenCrypter;
    private final Email email;
    private final AppUrlProvider urlProvider;


    public ServerAuthenticationService(ProfileService profiles, PasswordHashing passwordHashing, TokenCrypter tokenCrypter, Email email, AppUrlProvider urlProvider) {
        this.profiles = profiles;
        this.passwordHashing = passwordHashing;
        this.tokenCrypter = tokenCrypter;
        this.email = email;
        this.urlProvider = urlProvider;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public RequestPasswordResetResponse requestPasswordReset(RequestContext context, RequestPasswordResetRequest req) {
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
        return new RequestPasswordResetResponse();
    }

    @Override
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
        ApiToken apiToken = new ApiToken(profile.id, profile.roles);
        String token;
        try {
            token = apiToken.serialize(tokenCrypter);
        } catch (IOException e) {
            throw new HttpException.ServerErrorException("token generation error", e);
        }
        return token;
    }
}
