package team.catgirl.collar.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    private final CompletableFuture<HttpClientHandler> future;
    public HttpResponse response;
    public ByteBuffer contentBuffer = ByteBuffer.allocate(2000000);
    public Throwable error;

    public HttpClientHandler(CompletableFuture<HttpClientHandler> future) {
        this.future = future;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject o) {
        if (o instanceof HttpResponse) {
            response = (HttpResponse) o;
        }
        if (o instanceof HttpContent) {
            HttpContent content = (HttpContent) o;
            byte[] bytes = new byte[content.content().readableBytes()];
            content.content().readBytes(bytes);
            contentBuffer.put(bytes);
            this.future.complete(this);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.error = cause;
        ctx.close();
        this.future.complete(this);
    }
}
