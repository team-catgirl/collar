package team.catgirl.collar.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.ssl.SslContext;

class HttpClientInitializer extends ChannelInitializer<SocketChannel> {

    private final HttpClientHandler httpClientHandler;
    private final SslContext sslContext;
    private final String server;
    private final int port;

    public HttpClientInitializer(HttpClientHandler httpClientHandler, SslContext sslContext, String server, int port) {
        this.httpClientHandler = httpClientHandler;
        this.sslContext = sslContext;
        this.server = server;
        this.port = port;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (sslContext != null) {
            p.addLast(sslContext.newHandler(ch.alloc(), server, port));
        }
        p.addLast(new HttpClientCodec());
        p.addLast(new HttpContentDecompressor());
        p.addLast(httpClientHandler);
    }
}
