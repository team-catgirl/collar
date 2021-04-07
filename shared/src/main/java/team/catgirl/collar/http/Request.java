package team.catgirl.collar.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder.ErrorDataEncoderException;
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
    Map<String, String> form;

    private Request(URI uri) {
        this.uri = uri;
    }

    public Request get() {
        this.method = HttpMethod.GET;
        return this;
    }

    public Request post(Map<String, String> form) {
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

    HttpRequest create(ObjectMapper mapper) {
        if (method == null) {
            throw new IllegalStateException("method not set");
        }
        ByteBuf byteBuf;
        try {
            byteBuf = this.content == null ? Unpooled.EMPTY_BUFFER : Unpooled.copiedBuffer(mapper.writeValueAsBytes(content));
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
