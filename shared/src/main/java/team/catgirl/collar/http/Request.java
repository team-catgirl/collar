package team.catgirl.collar.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder.ErrorDataEncoderException;
import team.catgirl.collar.utils.Utils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public final class Request {

    public final HttpMethod method;
    public final URI uri;
    public final Object content;
    public final Map<String, String> headers;
    public final Map<String, String> form;

    public Request(HttpMethod method, URI uri, Object content, Map<String, String> headers, Map<String, String> form) {
        this.method = method;
        this.uri = uri;
        this.content = content;
        this.headers = headers;
        this.form = form;
    }

    HttpRequest create() {
        if (method == null) {
            throw new IllegalStateException("method not set");
        }
        ByteBuf byteBuf;
        try {
            byteBuf = this.content == null ? Unpooled.EMPTY_BUFFER : Unpooled.copiedBuffer(Utils.jsonMapper().writeValueAsBytes(content));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri.toString(), byteBuf);
        headers.forEach((name, value) -> request.headers().add(name, value));
        if (form != null) {
            HttpPostRequestEncoder encoder;
            try {
                encoder = new HttpPostRequestEncoder(request, false);
            } catch (ErrorDataEncoderException e) {
                throw new IllegalStateException(e);
            }
            form.forEach((name, value) -> {
                try {
                    encoder.addBodyAttribute(name, value);
                } catch (ErrorDataEncoderException e) {
                    throw new IllegalArgumentException(e);
                }
            });
            try {
                return encoder.finalizeRequest();
            } catch (ErrorDataEncoderException e) {
                throw new IllegalStateException(e);
            }
        }
        return request;
    }

    public static Builder url(UrlBuilder builder) {
        return new Builder(builder.toUri());
    }

    public static Builder url(String url) {
        return url(UrlBuilder.fromString(url));
    }

    public static class Builder {
        private final Map<String, String> headers = new HashMap<>();
        private final URI uri;

        private Builder(URI uri) {
            this.uri = uri;
        }

        public Builder addHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Request get() {
            return new Request(HttpMethod.GET, uri, null, headers, null);
        }

        public Request post(Map<String, String> form) {
            return new Request(HttpMethod.POST, uri, null, headers, form);
        }

        public Request post(Object content) {
            return new Request(HttpMethod.POST, uri, content, headers, null);
        }
    }
}
