package team.catgirl.collar.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.nio.ByteBuffer;

/**
 * WebSocket handle to send data through the socket
 */
public final class WebSocket {
    /**
     * Request used to create the WebSocket
     */
    public final Request request;
    private final Channel channel;

    WebSocket(Request request, Channel channel) {
        this.request = request;
        this.channel = channel;
    }

    /**
     * Send message to server
     * @param message to send
     */
    public void send(ByteBuffer message) {
        assertWritable();
        channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(message)));
    }

    /**
     * Send message to server
     * @param message to send
     */
    public void send(String message) {
        assertWritable();
        channel.writeAndFlush(new TextWebSocketFrame(message));
    }

    private void assertWritable() {
        if (!channel.isWritable()) {
            throw new IllegalStateException("socket is closed");
        }
    }

    public void close(int code, String reason) {
        channel.writeAndFlush(new CloseWebSocketFrame(code, reason));
    }

    public void close() {
        channel.close();
    }
}
