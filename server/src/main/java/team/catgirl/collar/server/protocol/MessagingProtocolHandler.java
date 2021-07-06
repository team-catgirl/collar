package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.messaging.SendMessageRequest;
import team.catgirl.collar.protocol.messaging.SendMessageResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.session.SessionManager;

import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagingProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = Logger.getLogger(MessagingProtocolHandler.class.getName());

    private final SessionManager sessions;
    private final GroupService groups;
    private final ServerIdentity serverIdentity;

    public MessagingProtocolHandler(SessionManager sessions, GroupService groups, ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.groups = groups;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof SendMessageRequest) {
            SendMessageRequest request = (SendMessageRequest) req;
            if (request.group != null) {
                groups.createMessages(request).ifPresent(response -> sender.accept(null, response));
            } else if (request.recipient != null) {
                sessions.findPlayer(request.identity).ifPresentOrElse(player -> {
                    sender.accept(request.recipient, new SendMessageResponse(this.serverIdentity, req.identity, null, player, request.message));
                }, () -> {
                    LOGGER.log(Level.INFO, "could not find player for " + req.identity);
                });
            } else {
                LOGGER.log(Level.WARNING, "sent a malformed SendMessageRequest by " + req.identity);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}
}
