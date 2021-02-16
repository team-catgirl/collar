package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.textures.GetTextureRequest;
import team.catgirl.collar.protocol.textures.GetTextureResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.textures.TextureService;
import team.catgirl.collar.server.services.textures.TextureService.FindTextureRequest;
import team.catgirl.collar.server.services.textures.TextureService.Texture;
import team.catgirl.collar.server.session.SessionManager;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TexturesProtocolHandler extends ProtocolHandler {
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
            // TODO: find player and return both its player and identity object? probably tons more performant...
            sessions.findPlayer(req.identity, request.player).ifPresent(player -> {
                sessions.getIdentity(player).ifPresent(identity -> {
                    try {
                        Texture texture = textures.findTexture(RequestContext.ANON, new FindTextureRequest(identity.owner, request.type)).texture;
                        sender.accept(request.identity, new GetTextureResponse(serverIdentity, texture.id, player, texture.url, texture.type));
                    } catch (NotFoundException ignored) {}
                });
            });
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, MinecraftPlayer player, BiConsumer<Session, ProtocolResponse> sender) {}
}
