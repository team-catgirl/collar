package team.catgirl.collar.server.http;

import io.mikael.urlbuilder.UrlBuilder;

/**
 * Provides the default URLs for the collar web app
 */
public class DefaultAppUrlProvider implements AppUrlProvider {
    public final UrlBuilder baseUrl;

    public DefaultAppUrlProvider(String baseUrl) {
        this.baseUrl = UrlBuilder.fromString(baseUrl);
    }

    @Override
    public String homeUrl() {
        return this.baseUrl.withPath("/").toString();
    }

    @Override
    public String loginUrl() {
        return this.baseUrl.withPath("/login").toString();
    }

    @Override
    public String signupUrl() {
        return this.baseUrl.withPath("/signup").toString();
    }

    @Override
    public String resetPrivateIdentity() {
        return this.baseUrl.withPath("/reset/").toString();
    }

    @Override
    public String deviceVerificationUrl(String token) {
        return this.baseUrl.withPath("/device/accept/" + token).toString();
    }

    @Override
    public String emailVerificationUrl(String token) {
        return this.baseUrl.withPath("/verify/" + token).toString();
    }

    @Override
    public String logoUrl(int size) {
        return this.baseUrl.withPath("/logo.png").toString();
    }

    @Override
    public String resetPassword(String token) {
        return this.baseUrl.withPath("/reset").toString();
    }
}
