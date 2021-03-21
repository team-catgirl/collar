package team.catgirl.collar.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import okhttp3.*;
import team.catgirl.collar.api.authentication.AuthenticationService;
import team.catgirl.collar.api.authentication.AuthenticationService.LoginRequest;
import team.catgirl.collar.api.authentication.AuthenticationService.LoginResponse;
import team.catgirl.collar.api.authentication.AuthenticationService.RequestPasswordResetRequest;
import team.catgirl.collar.api.authentication.AuthenticationService.RequestPasswordResetResponse;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.HttpException.BadRequestException;

import java.awt.*;
import java.io.IOException;
import java.net.SocketTimeoutException;

public class CollarApi {

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
        return doHttpPost(baseURL + "/auth/login", req, LoginResponse.class);
    }

    public void resetPassword(RequestPasswordResetRequest req) {
        doHttpPost(baseURL + "/auth/reset/request", req, RequestPasswordResetResponse.class);
    }

    private <T> T doHttpPost(String url, Object bodyObject, Class<T> responseClazz) {
        String bodyContent;
        try {
            bodyContent = mapper.writeValueAsString(bodyObject);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cant serialize", e);
        }
        RequestBody requestBody = RequestBody.create(MediaType.get("application/json"), bodyContent);
        Request.Builder request = prepareHeaders(new Request.Builder().url(url)).post(requestBody);
        try (Response response = http.newCall(request.build()).execute()) {
            final ResponseBody body = response.body();
            String responseBodyContent = body == null ? null : body.string();
            switch (response.code()) {
                case 400:
                    throw new BadRequestException(response.message() + " Body: " +  responseBodyContent);
                case 401:
                    throw new HttpException.UnauthorisedException(response.message());
                case 403:
                    throw new HttpException.ForbiddenException(response.message());
                case 404:
                    throw new HttpException.NotFoundException(response.message());
                case 409:
                    throw new HttpException.ConflictException(response.message());
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

    private Request.Builder prepareHeaders(Request.Builder builder) {
        return builder;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
