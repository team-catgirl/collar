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
import team.catgirl.collar.http.Response;
import team.catgirl.collar.http.Request;

public final class CollarApi {

    private final String baseURL;
    private final HttpClient http;
    private String token;

    public CollarApi(String baseURL, HttpClient http) {
        this.baseURL = baseURL;
        this.http = http;
    }

    public LoginResponse login(LoginRequest req) {
        Request.Builder url = Request.url(baseURL + "/auth/login");
        if (token != null) {
            url = url.addHeader("Authorization", "Bearer " + token);
        }
        return http.execute(url.post(req), Response.json(LoginResponse.class));
    }

    public void resetPassword(RequestPasswordResetRequest req) {
        Request.Builder url = Request.url(baseURL + "/auth/reset/request");
        if (token != null) {
            url = url.addHeader("Authorization", "Bearer " + token);
        }
        http.execute(url.post(req), Response.noContent());
    }

    public void updateProfile(ProfileService.UpdateProfileRequest req) {
        Request.Builder url = Request.url(baseURL + "/auth/reset/request");
        if (token != null) {
            url = url.addHeader("Authorization", "Bearer " + token);
        }
        http.execute(url.post(req), Response.noContent());
    }

    public GetProfileResponse getProfile(GetProfileRequest req) {
        UrlBuilder url = UrlBuilder.fromString(baseURL + "/profile")
                .addParameter("email", req.byEmail);
        return http.execute(Request.url(url).get(), Response.json(GetProfileResponse.class));
    }

    public void setToken(String token) {
        this.token = token;
    }
}
