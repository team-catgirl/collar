package team.catgirl.collar.server.common;

import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public final class ServerVersion {
    public final String version;
    public final String builtAt;

    public ServerVersion(String version, String builtAt) {
        this.version = version;
        this.builtAt = builtAt;
    }

    public static ServerVersion version() throws IOException {
        URL resource = Resources.getResource("version.properties");
        Properties properties = new Properties();
        try (InputStream is = resource.openStream()) {
            properties.load(is);
            String version = properties.getProperty("git.commit.id");
            String builtAt = properties.getProperty("git.build.time");
            return new ServerVersion(version, builtAt);
        }
    }
}
