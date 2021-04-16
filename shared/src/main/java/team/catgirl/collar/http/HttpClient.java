package team.catgirl.collar.http;

import com.google.common.io.BaseEncoding;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.HttpException.*;
import team.catgirl.collar.security.TokenGenerator;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple Netty based HTTP client
 */
public final class HttpClient implements Closeable {

    private static final int PORT_HTTP = 80;
    private static final int PORT_HTTPS = 443;
    private final EventLoopGroup group;
    private final SslContext sslContext;

    public HttpClient() {
        this(null);
    }

    /**
     * Create a HttpClient with a specific SSL configuration
     * @param sslContext to use
     */
    public HttpClient(SslContext sslContext) {
        if (sslContext == null) {
            try {
                this.sslContext = SslContextBuilder.forClient().build();
            } catch (SSLException e) {
                throw new IllegalStateException(e);
            }
        } else {
            this.sslContext = sslContext;
        }
        group = new NioEventLoopGroup();
    }

    /**
     * Create a WebSocket connection
     * @param request to make
     * @param listener to received messages from the socket
     * @return WebSocket reference
     */
    public WebSocket webSocket(Request request, WebSocketListener listener) {
        if (request.method != null) {
            throw new IllegalStateException("method should not be set for websocket");
        }
        int port = getPort(request);
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        String host = request.uri.getHost();
        request.headers.forEach(headers::add);
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                request.uri,
                WebSocketVersion.V13,
                null,
                false,
                headers,
                Short.MAX_VALUE);
        WebSocketClientHandler handler = new WebSocketClientHandler(request, handshaker, listener);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (sslContext != null && request.isSecure()) {
                            pipeline.addLast("ssl", sslContext.newHandler(ch.alloc(), host, port));
                        }
                        pipeline.addLast("http-codec", new HttpClientCodec());
//                        pipeline.addLast("decompressor", new HttpContentDecompressor());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                        pipeline.addLast("ws-handler", handler);
                    }
                });

        Channel channel;
        try {
            channel = bootstrap.connect(host, port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            handler.handshakeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new WebSocket(request, channel);
    }

    /**
     * Executes a {@link Request}
     * @param request to execute
     * @param response of the request
     * @param <T> return data type
     * @return response
     * @throws HttpException on non-200 range response
     */
    public <T> T execute(Request request, Response<T> response) {
        AtomicReference<T> httpResp = new AtomicReference<>();
        try {
            makeRequest(request).thenAccept(httpClientHandler -> {
                if (httpClientHandler.error != null) {
                    throw new RuntimeException(httpClientHandler.error);
                } else {
                    HttpResponseStatus resp = httpClientHandler.response.status();
                    int status = resp.code();
                    if (status >= 200 && status <= 299) {
                        httpResp.set(response.map(httpClientHandler.contentBuffer.array()));
                    } else {
                        switch (status) {
                            case 400:
                                throw new BadRequestException(resp.reasonPhrase());
                            case 401:
                                throw new UnauthorisedException(resp.reasonPhrase());
                            case 403:
                                throw new ForbiddenException(resp.reasonPhrase());
                            case 404:
                                throw new NotFoundException(resp.reasonPhrase());
                            case 409:
                                throw new ConflictException(resp.reasonPhrase());
                            case 500:
                                throw new ServerErrorException(resp.reasonPhrase());
                            default:
                                throw new UnmappedHttpException(resp.code(), resp.reasonPhrase());
                        }
                    }
                }
            }).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (e.getCause() instanceof HttpException) {
                throw (HttpException)e.getCause();
            } else {
                throw new RuntimeException(e);
            }
        }
        return httpResp.get();
    }

    /**
     * Makes the HTTP request
     * @param request to make
     * @return response handler
     */
    private CompletableFuture<HttpClientHandler> makeRequest(Request request) {
        CompletableFuture<HttpClientHandler> future = new CompletableFuture<>();
        int port = getPort(request);
        HttpClientHandler httpClientHandler = new HttpClientHandler(future);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int)TimeUnit.SECONDS.toMillis(10));
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast("encoder", new HttpRequestEncoder());
            }
        });
        String host = request.uri.getHost();
        bootstrap.handler(new HttpClientInitializer(httpClientHandler, request.isSecure() ? sslContext : null, host, port));
        Channel channel;
        try {
            channel = bootstrap.connect(host, port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        HttpRequest httpRequest = request.create();
        httpRequest.headers().set(HttpHeaderNames.HOST, host);
        httpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        httpRequest.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        channel.writeAndFlush(httpRequest);
        try {
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return future;
    }

    private int getPort(Request request) {
        int port = request.uri.getPort();
        if (port == -1) {
            if (request.isSecure()) {
                port = PORT_HTTPS;
            } else {
                port = PORT_HTTP;
            }
        }
        return port;
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }

}
