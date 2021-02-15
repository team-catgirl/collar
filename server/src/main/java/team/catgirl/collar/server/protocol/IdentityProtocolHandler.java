package team.catgirl.collar.server.protocol;

import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.identity.GetIdentityRequest;
import team.catgirl.collar.protocol.identity.GetIdentityResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.session.SessionManager;

import java.util.function.Consumer;

public class IdentityProtocolHandler extends ProtocolHandler {

    private final SessionManager sessions;
    private final ServerIdentity serverIdentity;

    public IdentityProtocolHandler(SessionManager sessions, ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, Consumer<ProtocolResponse> sender) {
        if (req instanceof GetIdentityRequest) {
            GetIdentityRequest request = (GetIdentityRequest) req;
            sessions.getIdentity(request.player).ifPresentOrElse(identity -> {
                sender.accept(new GetIdentityResponse(serverIdentity, request.id, identity));
            }, () -> {
                sender.accept(new GetIdentityResponse(serverIdentity, request.id, null));
            });
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopped(ClientIdentity identity, MinecraftPlayer player, Consumer<ProtocolResponse> sender) {}
}
