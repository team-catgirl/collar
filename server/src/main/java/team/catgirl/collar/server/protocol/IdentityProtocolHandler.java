package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.identity.CreateTrustRequest;
import team.catgirl.collar.protocol.identity.CreateTrustResponse;
import team.catgirl.collar.protocol.identity.GetIdentityRequest;
import team.catgirl.collar.protocol.identity.GetIdentityResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.session.SessionManager;

import java.util.function.BiConsumer;

public class IdentityProtocolHandler extends ProtocolHandler {

    private final SessionManager sessions;
    private final ServerIdentity serverIdentity;

    public IdentityProtocolHandler(SessionManager sessions, ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof GetIdentityRequest) {
            GetIdentityRequest request = (GetIdentityRequest) req;
            sessions.getIdentityByMinecraftPlayerId(request.player).ifPresentOrElse(identity -> {
                sender.accept(request.identity, new GetIdentityResponse(serverIdentity, request.id, identity));
            }, () -> {
                sender.accept(request.identity, new GetIdentityResponse(serverIdentity, request.id, null));
            });
            return true;
        } else if (req instanceof CreateTrustRequest) {
            CreateTrustRequest request = (CreateTrustRequest) req;
            sender.accept(request.recipient, new CreateTrustResponse(serverIdentity, request.id, request.preKeyBundle, request.identity));
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}
}
