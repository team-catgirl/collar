package team.catgirl.collar.server.http;

import io.mikael.urlbuilder.UrlBuilder;

public class DefaultAppUrlProvider implements AppUrlProvider {
    public final UrlBuilder baseUrl;

    public DefaultAppUrlProvider(String baseUrl) {
        this.baseUrl = UrlBuilder.fromString(baseUrl);
    }

    @Override
    public String deviceVerificationUrl(String token) {
        return this.baseUrl.withPath("/app/devices/trust/" + token).toString();
    }
}
