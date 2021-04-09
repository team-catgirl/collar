package team.catgirl.collar.http;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = Logger.getLogger(WebSocketClientHandler.class.getName());
    public static final int CLOSE_NORMAL = 1000;
    public static final int SERVER_ERROR = 1011;

    private final Request request;
    private final WebSocketClientHandshaker handshaker;
    private final WebSocketListener listener;
    private ChannelPromise handshakeFuture;
    private WebSocket webSocket;

    public WebSocketClientHandler(Request request, WebSocketClientHandshaker handshaker, WebSocketListener listener) {
        this.request = request;
        this.handshaker = handshaker;
        this.listener = listener;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
        webSocket = new WebSocket(request, ctx.channel());
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        listener.onClose(webSocket, CLOSE_NORMAL, "Socket closed after inactivity");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        final Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            handshakeFuture.setSuccess();
            listener.onOpen(webSocket);
            return;
        }
        if (msg instanceof FullHttpResponse) {
            ctx.close();
            listener.onClose(webSocket, SERVER_ERROR, "The specified endpoint is not a WebSocket");
        }
        final WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            listener.onMessage(webSocket, textFrame.text());
        } else if (frame instanceof CloseWebSocketFrame) {
            CloseWebSocketFrame closeFrame = (CloseWebSocketFrame) frame;
            ch.close();
            listener.onClose(webSocket, closeFrame.statusCode(), closeFrame.reasonText());
        } else if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
            ByteBuffer buffer = binaryFrame.content().nioBuffer();
            listener.onMessage(webSocket, buffer);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        listener.onFailure(null, cause);
        ctx.close();
        listener.onClose(null, SERVER_ERROR, cause.getMessage());
    }
}