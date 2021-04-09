package team.catgirl.collar.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;

import java.nio.ByteBuffer;

class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    public HttpResponse response;
    public ByteBuffer contentBuffer = ByteBuffer.allocate(2000000);
    public Throwable error;

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
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.error = cause;
        ctx.close();
    }
}
