package team.catgirl.collar.server.http;

import io.mikael.urlbuilder.UrlBuilder;

public class DefaultAppUrlProvider implements AppUrlProvider {
    public final UrlBuilder baseUrl;

    public DefaultAppUrlProvider(String baseUrl) {
        this.baseUrl = UrlBuilder.fromString(baseUrl);
    }

    @Override
    public String homeUrl() {
        return this.baseUrl.withPath("/app").toString();
    }

    @Override
    public String loginUrl() {
        return this.baseUrl.withPath("/app/login").toString();
    }

    @Override
    public String signupUrl() {
        return this.baseUrl.withPath("/app/signup").toString();
    }

    @Override
    public String resetPrivateIdentity() {
        return this.baseUrl.withPath("/app/reset/").toString();
    }

    @Override
    public String deviceVerificationUrl(String token) {
        return this.baseUrl.withPath("/app/devices/trust/" + token).toString();
    }

    @Override
    public String emailVerificationUrl(String token) {
        return this.baseUrl.withPath("/app/verify/").addParameter("token", token).toString();
    }

    @Override
    public String logoUrl(int size) {
        return this.baseUrl.withPath("/app/static/logo.png").toString();
    }

    @Override
    public String resetPassword(String token) {
        return this.baseUrl.withPath("/app/reset").toString();
    }
}
