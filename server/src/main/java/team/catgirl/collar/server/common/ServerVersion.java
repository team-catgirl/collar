package team.catgirl.collar.server.common;

import java.io.IOException;

public final class ServerVersion {
    public final String version;

    public ServerVersion(String version) {
        this.version = version;
    }

    public static ServerVersion version() throws IOException {
        return new ServerVersion("1.0-beta");
    }
}
