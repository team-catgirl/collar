package team.catgirl.collar.server.protocol;

import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.server.CollarServer;

import java.util.function.Consumer;

public abstract class ProtocolHandler {
    public abstract boolean handleRequest(CollarServer collar, ProtocolRequest req, Consumer<ProtocolResponse> sender);
}
