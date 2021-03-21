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

import java.awt.*;
import java.io.IOException;
import java.net.SocketTimeoutException;

public final class CollarApi {

    private final String baseURL;
    private final ObjectMapper mapper;
    private final OkHttpClient http;
    private String token;

    public CollarApi(String baseURL, ObjectMapper mapper, OkHttpClient http) {
        this.baseURL = baseURL;
        this.mapper = mapper;
        this.http = http;
    }

    public LoginResponse login(LoginRequest req) {
        return doHttpPost(baseURL + "/auth/login", req, LoginResponse.class, false);
    }

    public void resetPassword(RequestPasswordResetRequest req) {
        doHttpPost(baseURL + "/auth/reset/request", req, RequestPasswordResetResponse.class, false);
    }

    public void updateProfile(ProfileService.UpdateProfileRequest req) {
        doHttpPost(baseURL + "/auth/reset/request", req, UpdateProfileResponse.class, true);
    }

    public GetProfileResponse getProfile(GetProfileRequest req) {
        String url = UrlBuilder.fromString(baseURL + "/profile")
                .addParameter("email", req.byEmail)
                .toString();
        return doHttpGet(url, GetProfileResponse.class, true);
    }

    private <T> T doHttpPost(String url, Object bodyObject, Class<T> responseClazz, boolean withAuth) {
        String bodyContent;
        try {
            bodyContent = mapper.writeValueAsString(bodyObject);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cant serialize", e);
        }
        RequestBody requestBody = RequestBody.create(bodyContent, MediaType.get("application/json"));
        Request.Builder request = new Request.Builder().url(url).post(requestBody);
        if (withAuth) {
            request = request.addHeader("Authorization", "Bearer " + token);
        }
        try (Response response = http.newCall(request.build()).execute()) {
            final ResponseBody body = response.body();
            String responseBodyContent = body == null ? null : body.string();
            switch (response.code()) {
                case 400:
                    throw new BadRequestException(response.message() + " Body: " +  responseBodyContent);
                case 401:
                    throw new UnauthorisedException(response.message());
                case 403:
                    throw new ForbiddenException(response.message());
                case 404:
                    throw new NotFoundException(response.message());
                case 409:
                    throw new ConflictException(response.message());
                case 200:
                    if (responseBodyContent == null) throw new IllegalStateException("body is null");
                    return mapper.readValue(responseBodyContent, responseClazz);
                default:
                    throw new HttpException.UnmappedHttpException(response.code(), response.message());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not map " + responseClazz.getName(), e);
        }
    }

    private <T> T doHttpGet(String url, Class<T> responseClazz, boolean withAuth) {
        Request.Builder request = new Request.Builder().url(url);
        if (withAuth) {
            request = request.addHeader("Authorization", "Bearer " + token);
        }
        try (Response response = http.newCall(request.build()).execute()) {
            final ResponseBody body = response.body();
            final String stringBody = body == null ? "" : body.string();
            switch (response.code()) {
                case 400:
                    throw new BadRequestException(response.message() + " Body: " +  stringBody);
                case 401:
                    throw new UnauthorisedException(response.message());
                case 403:
                    throw new ForbiddenException(response.message());
                case 404:
                    throw new NotFoundException(response.message());
                case 200:
                    if (body == null) throw new IllegalStateException("body is null");
                    return mapper.readValue(stringBody, responseClazz);
                default:
                    throw new HttpException.UnmappedHttpException(response.code(), response.message());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not map " + responseClazz.getName(), e);
        }
    }

    public void setToken(String token) {
        this.token = token;
    }
}
