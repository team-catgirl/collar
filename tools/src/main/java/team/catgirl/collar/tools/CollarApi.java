package team.catgirl.collar.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import team.catgirl.collar.api.authentication.AuthenticationService.LoginRequest;
import team.catgirl.collar.api.authentication.AuthenticationService.LoginResponse;
import team.catgirl.collar.api.authentication.AuthenticationService.RequestPasswordResetRequest;
import team.catgirl.collar.api.profiles.ProfileService;
import team.catgirl.collar.api.profiles.ProfileService.GetProfileRequest;
import team.catgirl.collar.api.profiles.ProfileService.GetProfileResponse;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.http.Request;

public final class CollarApi {

    private final String baseURL;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private String token;

    public CollarApi(String baseURL, ObjectMapper mapper, HttpClient http) {
        this.baseURL = baseURL;
        this.mapper = mapper;
        this.http = http;
    }

    public LoginResponse login(LoginRequest req) {
        return http.http(Request.url(baseURL + "/auth/login").post(req), LoginResponse.class);
    }

    public void resetPassword(RequestPasswordResetRequest req) {
        http.http(Request.url(baseURL + "/auth/reset/request").post(req), Void.class);
    }

    public void updateProfile(ProfileService.UpdateProfileRequest req) {
        http.http(Request.url(baseURL + "/auth/reset/request").post(req), Void.class);
    }

    public GetProfileResponse getProfile(GetProfileRequest req) {
        UrlBuilder url = UrlBuilder.fromString(baseURL + "/profile")
                .addParameter("email", req.byEmail);
        return http.http(Request.url(url).get(), GetProfileResponse.class);
    }

    public void setToken(String token) {
        this.token = token;
    }
}
