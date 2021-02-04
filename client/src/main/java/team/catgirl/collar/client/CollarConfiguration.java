package team.catgirl.collar.client;

import com.google.common.base.MoreObjects;
import team.catgirl.collar.api.location.Position;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CollarConfiguration {

    private static final Logger LOGGER = Logger.getLogger(CollarConfiguration.class.getName());

    public final Supplier<Position> playerPosition;
    public final Supplier<MinecraftSession> sessionSupplier;
    public final HomeDirectory homeDirectory;
    public final URL collarServerURL;
    public final CollarListener listener;

    private CollarConfiguration(Supplier<Position> playerPosition, Supplier<MinecraftSession> sessionSupplier, HomeDirectory homeDirectory, URL collarServerURL, CollarListener listener) {
        this.playerPosition = playerPosition;
        this.sessionSupplier = sessionSupplier;
        this.homeDirectory = homeDirectory;
        this.collarServerURL = collarServerURL;
        this.listener = listener;
    }

    public final static class Builder {
        private CollarListener listener;
        private Supplier<Position> playerPosition;
        private Supplier<MinecraftSession> sessionSupplier;
        private File homeDirectory;
        private URL collarServerURL;

        public Builder() {}

        /**
         * @param listener for client lifecycle
         * @return builder
         */
        public Builder withListener(CollarListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Use an alternate collar server
         * @param collarServerURL server url
         * @return builder
         */
        public Builder withCollarServer(URL collarServerURL) {
            this.collarServerURL = collarServerURL;
            return this;
        }

        /**
         * Use an alternate collar server
         * @param collarServerURL server url
         * @return builder
         */
        public Builder withCollarServer(String collarServerURL) {
            try {
                this.collarServerURL = new URL(collarServerURL);
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        /**
         * Use the official collar server
         * @return builder
         */
        public Builder withCollarServer() {
            withCollarServer("https://api.collarmc.com/");
            return this;
        }

        /**
         * Use the official collar development server
         * @return builder
         */
        public Builder withCollarDevelopmentServer() {
            withCollarServer("https://api.collarmc.com/");
            return this;
        }

        /**
         * Where to store the collar directory. Usually this is the minecraft home directory
         * @param homeDirectory to store collar state
         * @return builder
         */
        public Builder withHomeDirectory(File homeDirectory) {
            this.homeDirectory = homeDirectory;
            return this;
        }

        /**
         * Supply the players current position
         * @param playerPosition supplier
         * @return builder
         */
        public Builder withPlayerPosition(Supplier<Position> playerPosition) {
            this.playerPosition = playerPosition;
            return this;
        }

        /**
         * Do not verify the minecraft session on the server.
         * To use this the server must be setup with the NoJang auth scheme.
         * @param uuid of the minecraft player
         * @param server the minecraft server the player is logged into
         * @return builder
         */
        public Builder withNoJangAuthentication(UUID uuid, String server) {
            this.sessionSupplier = () -> MinecraftSession.from(uuid, null,  null, server);
            return this;
        }

        /**
         * Supplies the Mojang session information for the logged in user
         * The supplier should never cache this information
         * @param mojangAuthentication session supplier
         * @return builder
         */
        public Builder withMojangAuthentication(Supplier<MinecraftSession> mojangAuthentication) {
            this.sessionSupplier = mojangAuthentication;
            return this;
        }

        /**
         * Builds the new configuration
         * @return configuration of the collar client
         * @throws IOException if collar state directories cant be created
         */
        public CollarConfiguration build() throws IOException {
            Objects.requireNonNull(listener, "Collar listener was not set");
            Objects.requireNonNull(collarServerURL, "Collar server URL must be set");
            Objects.requireNonNull(homeDirectory, "Minecraft home directory must be set");
            Objects.requireNonNull(sessionSupplier, "Session supplier not set");
            HomeDirectory from = HomeDirectory.from(homeDirectory, collarServerURL.getHost());
            Supplier<Position> playerPosition = MoreObjects.firstNonNull(this.playerPosition, () -> {
                LOGGER.log(Level.WARNING, "Location features are disabled. Consumer did not provide a player position supplier");
                return Position.UNKNOWN;
            });
            return new CollarConfiguration(playerPosition, sessionSupplier, from, collarServerURL, listener);
        }
    }
}
