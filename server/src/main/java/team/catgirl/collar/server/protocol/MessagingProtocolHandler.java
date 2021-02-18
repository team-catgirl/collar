package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.messaging.SendMessageRequest;
import team.catgirl.collar.protocol.messaging.SendMessageResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.session.SessionManager;

import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagingProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = Logger.getLogger(MessagingProtocolHandler.class.getName());

    private final SessionManager sessions;
    private final ServerIdentity serverIdentity;

    public MessagingProtocolHandler(SessionManager sessions, ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof SendMessageRequest) {
            SendMessageRequest request = (SendMessageRequest) req;
            sessions.findPlayer(request.identity).ifPresentOrElse(player -> {
                sender.accept(request.individual, new SendMessageResponse(this.serverIdentity, req.identity, request.group, player, request.message));
            }, () -> {
                LOGGER.log(Level.INFO,"Could not find player for " + req.identity);
            });
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, MinecraftPlayer player, BiConsumer<Session, ProtocolResponse> sender) {}
}
