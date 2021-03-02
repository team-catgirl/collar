package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.textures.GetTextureRequest;
import team.catgirl.collar.protocol.textures.GetTextureResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.textures.TextureService;
import team.catgirl.collar.server.services.textures.TextureService.FindTextureRequest;
import team.catgirl.collar.server.services.textures.TextureService.Texture;
import team.catgirl.collar.server.session.SessionManager;

import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TexturesProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = Logger.getLogger(TexturesProtocolHandler.class.getName());

    private final ServerIdentity serverIdentity;
    private final SessionManager sessions;
    private final TextureService textures;

    public TexturesProtocolHandler(ServerIdentity serverIdentity, SessionManager sessions, TextureService textures) {
        this.serverIdentity = serverIdentity;
        this.sessions = sessions;
        this.textures = textures;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof GetTextureRequest) {
            GetTextureRequest request = (GetTextureRequest) req;
            if (request.player != null) {
                sessions.getSessionStateByOwner(request.player).ifPresent(sessionState -> {
                    try {
                        Texture texture = textures.findTexture(RequestContext.ANON, new FindTextureRequest(sessionState.identity.owner, request.group, request.type)).texture;
                        sender.accept(request.identity, new GetTextureResponse(serverIdentity, texture.id, null, sessionState.toPlayer(), texture.url, texture.type));
                    } catch (NotFoundException ignored) {
                        LOGGER.log(Level.INFO, "Could not find texture " + request.type + " for player " + request.player);
                    }
                });
            } else if (request.group != null) {
                try {
                    Texture texture = textures.findTexture(RequestContext.ANON, new FindTextureRequest(null, request.group, request.type)).texture;
                    sender.accept(request.identity, new GetTextureResponse(serverIdentity, texture.id, texture.group, null, texture.url, texture.type));
                } catch (NotFoundException ignored) {
                    LOGGER.log(Level.INFO, "Could not find texture " + request.type + " for group " + request.group);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) {}
}
