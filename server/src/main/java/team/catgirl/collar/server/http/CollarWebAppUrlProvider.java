package team.catgirl.collar.server.http;

import io.mikael.urlbuilder.UrlBuilder;

public class CollarWebAppUrlProvider implements AppUrlProvider {

    private final UrlBuilder builder;

    public CollarWebAppUrlProvider(String baseUrl) {
        this.builder = UrlBuilder.fromString(baseUrl);
    }

    @Override
    public String logoUrl(int size) {
        return builder.withPath("/static/logo.png").toString();
    }

    @Override
    public String homeUrl() {
        return builder.withPath("/").toString();
    }

    @Override
    public String loginUrl() {
        return builder.withPath("/login").toString();
    }

    @Override
    public String signupUrl() {
        return homeUrl();
    }

    @Override
    public String emailVerificationUrl(String token) {
        return builder.withPath("/verify/" + token).toString();
    }

    @Override
    public String resetPassword(String token) {
        return builder.withPath("/reset-password").toString();
    }

    @Override
    public String resetPrivateIdentity() {
        return builder.withPath("/reset").toString();
    }

    @Override
    public String deviceVerificationUrl(String token) {
        return builder.withPath("/device/accept/" + token).toString();
    }
}
