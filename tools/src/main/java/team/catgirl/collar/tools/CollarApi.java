package team.catgirl.collar.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import io.mikael.urlbuilder.UrlBuilder;
import okhttp3.*;
import team.catgirl.collar.api.authentication.AuthenticationService;
import team.catgirl.collar.api.authentication.AuthenticationService.LoginRequest;
import team.catgirl.collar.api.authentication.AuthenticationService.LoginResponse;
import team.catgirl.collar.api.authentication.AuthenticationService.RequestPasswordResetRequest;
import team.catgirl.collar.api.authentication.AuthenticationService.RequestPasswordResetResponse;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.HttpException.*;
import team.catgirl.collar.api.profiles.ProfileService;
import team.catgirl.collar.api.profiles.ProfileService.GetProfileRequest;
import team.catgirl.collar.api.profiles.ProfileService.GetProfileResponse;
import team.catgirl.collar.api.profiles.ProfileService.UpdateProfileResponse;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.http.Request;

import java.awt.*;
import java.io.IOException;
import java.net.SocketTimeoutException;

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
        return http.json(Request.url(baseURL + "/auth/login").post(req), LoginResponse.class);
    }

    public void resetPassword(RequestPasswordResetRequest req) {
        http.json(Request.url(baseURL + "/auth/reset/request").post(req), Void.class);
    }

    public void updateProfile(ProfileService.UpdateProfileRequest req) {
        http.json(Request.url(baseURL + "/auth/reset/request").post(req), Void.class);
    }

    public GetProfileResponse getProfile(GetProfileRequest req) {
        UrlBuilder url = UrlBuilder.fromString(baseURL + "/profile")
                .addParameter("email", req.byEmail);
        return http.json(Request.url(url).get(), GetProfileResponse.class);
    }

    public void setToken(String token) {
        this.token = token;
    }
}
