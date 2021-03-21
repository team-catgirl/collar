package team.catgirl.collar.api.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.api.profiles.PublicProfile;

public interface AuthenticationService {
    CreateAccountResponse createAccount(RequestContext context, CreateAccountRequest req);

    LoginResponse login(RequestContext context, LoginRequest req);

    VerifyAccountResponse verify(RequestContext context, VerifyAccountRequest req);

    RequestPasswordResetResponse requestPasswordReset(RequestContext context, RequestPasswordResetRequest req);

    ResetPasswordResponse resetPassword(RequestContext context, ResetPasswordRequest req);

    class CreateAccountRequest {
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

    class CreateAccountResponse {
        @JsonProperty("profile")
        public final PublicProfile profile;
        @JsonProperty("token")
        public final String token;

        public CreateAccountResponse(@JsonProperty("profile") PublicProfile profile,
                                     @JsonProperty("token") String token) {
            this.profile = profile;
            this.token = token;
        }
    }

    class LoginRequest {
        @JsonProperty("email")
        public final String email;
        @JsonProperty("password")
        public final String password;

        public LoginRequest(@JsonProperty("email") String email,
                            @JsonProperty("password") String password) {
            this.email = email;
            this.password = password;
        }
    }

    class LoginResponse {
        @JsonProperty("profile")
        public final Profile profile;
        @JsonProperty("token")
        public final String token;

        public LoginResponse(@JsonProperty("profile") Profile profile,
                             @JsonProperty("token") String token) {
            this.profile = profile;
            this.token = token;
        }
    }

    class VerifyAccountRequest {
        @JsonProperty("token")
        public final String token;

        public VerifyAccountRequest(@JsonProperty("token") String token) {
            this.token = token;
        }
    }

    class VerifyAccountResponse {
        @JsonProperty("redirectUrl")
        public final String redirectUrl;

        public VerifyAccountResponse(@JsonProperty("redirectUrl") String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }
    }

    class RequestPasswordResetRequest {
        @JsonProperty("email")
        public final String email;

        public RequestPasswordResetRequest(@JsonProperty("email") String email) {
            this.email = email;
        }
    }

    class RequestPasswordResetResponse {}

    class ResetPasswordRequest {
        @JsonProperty("token")
        public final String token;
        @JsonProperty("oldPassword")
        public final String oldPassword;
        @JsonProperty("newPassword")
        public final String newPassword;

        public ResetPasswordRequest(@JsonProperty("token") String token,
                                    @JsonProperty("oldPassword") String oldPassword,
                                    @JsonProperty("newPassword") String newPassword) {
            this.token = token;
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }
    }

    class ResetPasswordResponse {
        @JsonProperty("profile")
        public final PublicProfile profile;

        public ResetPasswordResponse(@JsonProperty("profile") PublicProfile profile) {

            this.profile = profile;
        }
    }
}
