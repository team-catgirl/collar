package team.catgirl.collar.http;

import java.nio.ByteBuffer;

/**
 * Listener for {@link HttpClient#webSocket(Request, WebSocketListener)}
 */
public interface WebSocketListener {
    /**
     * Socket opened
     * @param webSocket socket
     */
    default void onOpen(WebSocket webSocket) {};

    /**
     * Fired when the socket is closed
     * @param webSocket socket
     * @param code of close
     * @param message of close
     */
    default void onClose(WebSocket webSocket, int code, String message) {};

    /**
     * Fired when text frame received
     * @param webSocket socket
     * @param message received frame
     */
    default void onMessage(WebSocket webSocket, String message) {};

    /**
     * Fired when binary frame received
     * @param webSocket socket
     * @param messageBuffer received frame
     */
    default void onMessage(WebSocket webSocket, ByteBuffer messageBuffer) {};

    /**
     * Fired when the socket failed
     * @param webSocket socket
     * @param throwable of failure
     */
    default void onFailure(WebSocket webSocket, Throwable throwable) {};
}
