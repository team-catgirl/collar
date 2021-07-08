package team.catgirl.collar.client.debug;

import team.catgirl.collar.client.HomeDirectory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

/**
 * Debugging tools for collar developers
 * To use place a file containing java properties at $minecraftHome/collar/debug
 */
public final class DebugConfiguration {
    public final boolean tracers;
    public final boolean waypoints;
    public final Optional<URL> serverUrl;

    public DebugConfiguration(boolean tracers, boolean waypoints, URL serverUrl) {
        this.tracers = tracers;
        this.waypoints = waypoints;
        this.serverUrl = Optional.ofNullable(serverUrl);
    }

    public static DebugConfiguration load(HomeDirectory home) throws IOException {
        if (!home.debugFile().exists()) {
            return new DebugConfiguration(false, false, null);
        }
        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(home.debugFile())) {
            properties.load(is);
        }
        String url = properties.getProperty("server.url", null);
        return new DebugConfiguration(
                (boolean) properties.getOrDefault("tracers", false),
                (boolean) properties.getOrDefault("waypoints", false),
                url == null ? null : new URL(url)
        );
    }
}
