package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.CollarServer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Extensible protocol packet listener and sender
 */
public abstract class ProtocolHandler {
    /**
     * Handles a request coming from a client and processes it
     * @param collar server
     * @param req request received
     * @param sender to send a response
     * @return if packet handled
     */
    public abstract boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender);

    /**
     * Fired before the session is stopped and all session state information is removed from {@link team.catgirl.collar.server.session.SessionManager}
     * @param identity for which the session is being stopped
     * @param player for which the sesssion is being stopped
     * @param sender to send responses to clients
     */
    public abstract void onSessionStopping(ClientIdentity identity, MinecraftPlayer player, BiConsumer<Session, ProtocolResponse> sender);
}
