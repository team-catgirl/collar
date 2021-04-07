package team.catgirl.collar.http;

import io.mikael.urlbuilder.UrlBuilder;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import team.catgirl.collar.security.mojang.ServerAuthentication;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public final class Request {
    public final Map<String, String> headers = new HashMap<>();
    public final URI uri;
    private HttpMethod method;
    Object content;
    Map<String, Object> form;

    private Request(URI uri) {
        this.uri = uri;
    }

    public Request get() {
        this.method = HttpMethod.GET;
        return this;
    }

    public Request post(Map<String, Object> form) {
        this.method = HttpMethod.POST;
        this.form = form;
        return this;
    }

    public Request post(Object content) {
        this.method = HttpMethod.POST;
        this.content = content;
        return this;
    }

    public Request addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    HttpRequest create() {
        if (method == null) {
            throw new IllegalStateException("method not set");
        }
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri.toString(), Unpooled.EMPTY_BUFFER);
        headers.forEach((name, value) -> request.headers().add(name, value));
        return request;
    }

    public static Request url(UrlBuilder builder) {
        return new Request(builder.toUri());
    }

    public static Request url(String url) {
        try {
            return new Request(new URI(url));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
