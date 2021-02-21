package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.entities.UpdateEntitiesRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.services.entities.EntitiesService;
import team.catgirl.collar.server.session.SessionManager;

import java.util.function.BiConsumer;

public class EntitiesProtocolHandler extends ProtocolHandler {

    private final EntitiesService entities;

    public EntitiesProtocolHandler(EntitiesService entities) {
        this.entities = entities;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof UpdateEntitiesRequest) {
            UpdateEntitiesRequest request = (UpdateEntitiesRequest) req;
            BatchProtocolResponse response = entities.updateEntities(request);
            sender.accept(null, response);
            return true;
        }
        return false;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, MinecraftPlayer player, BiConsumer<Session, ProtocolResponse> sender) {}
}
