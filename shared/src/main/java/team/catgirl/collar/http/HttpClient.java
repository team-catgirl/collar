package team.catgirl.collar.http;

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

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * Simple Netty based HTTP client
 */
public final class HttpClient implements Closeable {

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
        int port = getPort(request);
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.HOST, request.uri.getHost());
        headers.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
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
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (sslContext != null && request.isSecure()) {
                            pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
                        }
                        pipeline.addLast("http-codec", new HttpClientCodec());
                        pipeline.addLast("decompressor", new HttpContentDecompressor());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                        pipeline.addLast("ws-handler", handler);
                    }
                });

        Channel channel;
        try {
            channel = bootstrap.connect(request.uri.getHost(), port).sync().channel();
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
        HttpClientHandler httpClientHandler = makeRequest(request);
        if (httpClientHandler.error != null) {
            throw new RuntimeException(httpClientHandler.error);
        } else {
            HttpResponseStatus resp = httpClientHandler.response.status();
            int status = resp.code();
            if (status >= 200 && status <= 299) {
                return response.map(httpClientHandler.contentBuffer.array());
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
    }

    /**
     * Makes the HTTP request
     * @param request to make
     * @return response handler
     */
    private HttpClientHandler makeRequest(Request request) {
        int port = getPort(request);
        HttpClientHandler httpClientHandler = new HttpClientHandler();
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
        bootstrap.handler(new HttpClientInitializer(httpClientHandler, request.isSecure() ? sslContext : null));
        String host = request.uri.getHost();
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
        return httpClientHandler;
    }

    private int getPort(Request request) {
        int port = request.uri.getPort();
        if (port == -1) {
            if (request.isSecure()) {
                port = 444;
            } else {
                port = 80;
            }
        }
        return port;
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }

}
