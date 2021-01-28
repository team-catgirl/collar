package team.catgirl.collar.server.common;

import java.io.IOException;

public final class ServerVersion {
    public final int major;
    public final int minor;
    public final int patch;

    public ServerVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static ServerVersion version() throws IOException {
        return new ServerVersion(1, 0, 0);
    }
}
