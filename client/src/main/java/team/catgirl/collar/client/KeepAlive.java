package team.catgirl.collar.client;

import okhttp3.WebSocket;
import team.catgirl.collar.client.Collar.CollarWebSocket;
import team.catgirl.collar.client.CollarException.ConnectionException;
import team.catgirl.collar.protocol.keepalive.KeepAliveRequest;
import team.catgirl.collar.security.ClientIdentity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Schedules a {@link KeepAliveRequest} for the connection lifecycle of a {@link WebSocket}
 */
final class KeepAlive {
    private static final Logger LOGGER = Logger.getLogger(KeepAlive.class.getName());
    private final WebSocket webSocket;
    private ScheduledExecutorService scheduler;
    private final CollarWebSocket collarWebSocket;

    public KeepAlive(CollarWebSocket collarWebSocket, WebSocket webSocket) {
        this.collarWebSocket = collarWebSocket;
        this.webSocket = webSocket;
    }

    public void start(ClientIdentity identity) {
        scheduler = Executors.newScheduledThreadPool(1);
//        scheduler.scheduleAtFixedRate(() -> {
//            try {
//                KeepAliveRequest keepAliveRequest = new KeepAliveRequest(identity);
//                collarWebSocket.sendRequest(webSocket, keepAliveRequest);
//            } catch (ConnectionException e) {
//                LOGGER.log(Level.SEVERE, "Couldn't send KeepAliveRequest", e);
//                webSocket.close(1000, "Keep alive failed");
//            }
//        }, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }
    }
}
