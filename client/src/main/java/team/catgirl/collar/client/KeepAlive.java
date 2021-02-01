package team.catgirl.collar.client;

import okhttp3.WebSocket;
import team.catgirl.collar.protocol.keepalive.KeepAliveRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
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

    public KeepAlive(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public void start(ClientIdentity identity) {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                webSocket.send(Utils.createObjectMapper().writeValueAsString(new KeepAliveRequest(identity)));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Couldn't send KeepAliveRequest", e);
                webSocket.close(1000, "Keep alive failed");
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }
    }
}
