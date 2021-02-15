package team.catgirl.collar.server.protocol;

import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.messaging.SendMessageRequest;
import team.catgirl.collar.protocol.messaging.SendMessageResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.session.SessionManager;

import java.util.function.Consumer;

public class MessagingProtocolHandler extends ProtocolHandler {
    private final SessionManager sessions;
    private final ServerIdentity serverIdentity;

    public MessagingProtocolHandler(SessionManager sessions, ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, Consumer<ProtocolResponse> sender) {
        if (req instanceof SendMessageRequest) {
            SendMessageRequest request = (SendMessageRequest) req;
            sessions.findPlayer(request.identity).ifPresent(player -> {
                sender.accept(new SendMessageResponse(this.serverIdentity, req.identity, player, request.message));
            });
        }
        return false;
    }

    @Override
    public void onSessionStopped(ClientIdentity identity, MinecraftPlayer player, Consumer<ProtocolResponse> sender) {

    }
}
