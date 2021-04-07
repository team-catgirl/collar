package team.catgirl.collar.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import team.catgirl.collar.api.http.HttpException.*;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class HttpClient implements Closeable {

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final ObjectMapper mapper;
    private final SslContext sslContext;

    public HttpClient(ObjectMapper mapper, SslContext sslContext) {
        this.mapper = mapper;
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
        bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast("encoder", new HttpRequestEncoder());
            }
        });
    }

    public byte[] bytes(Request request) throws IOException, UnauthorisedException {
        ClientHandler clientHandler = json(request);
        if (clientHandler.error != null) {
            throw new RuntimeException(clientHandler.error);
        } else {
            HttpResponseStatus resp = clientHandler.response.status();
            int status = resp.code();
            if (status >= 200 && status <= 299) {
                return clientHandler.content.content().array();
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

    public <T> T json(Request request, Class<T> aClass) {
        ClientHandler clientHandler = json(request);
        if (clientHandler.error != null) {
            throw new RuntimeException(clientHandler.error);
        } else {
            HttpResponseStatus resp = clientHandler.response.status();
            int status = resp.code();
            if (status >= 200 && status <= 299) {
                try {
                    return mapper.readValue(clientHandler.content.content().toString(StandardCharsets.UTF_8), aClass);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
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

    private ClientHandler json(Request request) {
        String scheme = request.uri.getScheme() == null? "http" : request.uri.getScheme();
        int port = request.uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }
        ClientHandler clientHandler = new ClientHandler();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ClientInitializer(clientHandler, port == 433 ? sslContext : null));

        Channel channel;
        try {
            channel = bootstrap.connect(request.uri.getHost(), port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        HttpRequest httpRequest = request.create();
        httpRequest.headers().set(HttpHeaderNames.HOST, request.uri.getHost());
        httpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        httpRequest.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

        channel.writeAndFlush(httpRequest);
        try {
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return clientHandler;
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }

    private static class ClientInitializer extends ChannelInitializer<SocketChannel> {

        private final ClientHandler clientHandler;
        private final SslContext sslContext;

        public ClientInitializer(ClientHandler clientHandler, SslContext sslContext) {
            this.clientHandler = clientHandler;
            this.sslContext = sslContext;
        }

        @Override
        public void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            if (sslContext != null) {
                p.addLast(sslContext.newHandler(ch.alloc()));
            }
            p.addLast(new HttpClientCodec());
            p.addLast(new HttpContentDecompressor());
            p.addLast(clientHandler);
        }
    }

    private static class ClientHandler extends SimpleChannelInboundHandler<HttpObject> {
        public HttpResponse response;
        public HttpContent content;
        public Throwable error;

        @Override
        public void channelRead0(ChannelHandlerContext ctx, HttpObject o) {
            if (o instanceof HttpResponse) {
                response = (HttpResponse) o;
            }
            if (o instanceof HttpContent) {
                content = (HttpContent) o;
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            this.error = cause;
            ctx.close();
        }
    }
}
