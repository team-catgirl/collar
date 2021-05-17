package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.api.profiles.ProfileService;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.identity.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.session.SessionManager;

import java.util.function.BiConsumer;

public class IdentityProtocolHandler extends ProtocolHandler {

    private final SessionManager sessions;
    private final ProfileService profiles;
    private final ServerIdentity serverIdentity;

    public IdentityProtocolHandler(SessionManager sessions,
                                   ProfileService profiles,
                                   ServerIdentity serverIdentity) {
        this.sessions = sessions;
        this.profiles = profiles;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar,
                                 ProtocolRequest req,
                                 BiConsumer<ClientIdentity, ProtocolResponse> sender) {
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
        } else if (req instanceof GetProfileRequest) {
            GetProfileRequest request = (GetProfileRequest) req;
            PublicProfile profile;
            try {
                profile = profiles.getProfile(RequestContext.SERVER, ProfileService.GetProfileRequest.byId(request.profile)).profile.toPublic();
            } catch (HttpException.NotFoundException e) {
                profile = null;
            }
            sender.accept(request.identity, new GetProfileResponse(serverIdentity, request.profile, profile));
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}
}
