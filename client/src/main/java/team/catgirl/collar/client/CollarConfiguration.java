package team.catgirl.collar.client;

import com.google.common.base.MoreObjects;
import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.client.debug.DebugConfiguration;
import team.catgirl.collar.client.minecraft.Ticks;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CollarConfiguration {

    private static final Logger LOGGER = Logger.getLogger(CollarConfiguration.class.getName());

    public final Supplier<Location> playerLocation;
    public final Supplier<MinecraftSession> sessionSupplier;
    public final Supplier<Set<Entity>> entitiesSupplier;
    public final HomeDirectory homeDirectory;
    public final DebugConfiguration debugConfiguration;
    public final URL collarServerURL;
    public final CollarListener listener;
    public final Ticks ticks;
    public final boolean debugMode;

    private CollarConfiguration(Supplier<Location> playerLocation,
                                Supplier<MinecraftSession> sessionSupplier,
                                Supplier<Set<Entity>> entitiesSupplier,
                                HomeDirectory homeDirectory,
                                DebugConfiguration debugConfiguration, URL collarServerURL,
                                CollarListener listener,
                                Ticks ticks) {
        this.playerLocation = playerLocation;
        this.sessionSupplier = sessionSupplier;
        this.entitiesSupplier = entitiesSupplier;
        this.homeDirectory = homeDirectory;
        this.debugConfiguration = debugConfiguration;
        this.collarServerURL = collarServerURL;
        this.listener = listener;
        this.ticks = ticks;
        this.debugMode = homeDirectory.debugFile().exists();
    }

    public final static class Builder {
        private CollarListener listener;
        private Supplier<Location> playerLocation;
        private Supplier<MinecraftSession> sessionSupplier;
        private Supplier<Set<Entity>> entitiesSupplier;
        private File homeDirectory;
        private URL collarServerURL;
        private Ticks ticks;

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
            withCollarServer("https://dev.api.collarmc.com/");
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
         * Supply the players current location
         * @param playerLocation supplier
         * @return builder
         */
        public Builder withPlayerLocation(Supplier<Location> playerLocation) {
            this.playerLocation = playerLocation;
            return this;
        }

        /**
         * Supply the nearby entity list of the player
         * @param entitiesSupplier supplier
         * @return builder
         */
        public Builder withEntitiesSupplier(Supplier<Set<Entity>> entitiesSupplier) {
            this.entitiesSupplier = entitiesSupplier;
            return this;
        }

        /**
         * Supplies the session information for the logged in user
         * The supplier should never cache this information
         * @param sessionSupplier session supplier
         * @return builder
         */
        public Builder withSession(Supplier<MinecraftSession> sessionSupplier) {
            this.sessionSupplier = sessionSupplier;
            return this;
        }

        /**
         * Provides Collar with Minecraft client tick events
         * @param ticks ticks
         * @return builder
         */
        public Builder withTicks(Ticks ticks) {
            this.ticks = ticks;
            return this;
        }

        /**
         * Builds the new configuration
         * @return configuration of the collar client
         * @throws IOException if collar state directories cant be created
         */
        public CollarConfiguration build() throws IOException {
            HomeDirectory homeDirectory = HomeDirectory.from(this.homeDirectory, collarServerURL.getHost());
            Objects.requireNonNull(listener, "Collar listener was not set");
            Objects.requireNonNull(collarServerURL, "Collar server URL must be set");
            Objects.requireNonNull(homeDirectory, "Minecraft home directory must be set");
            Objects.requireNonNull(sessionSupplier, "Session supplier not set");
            Objects.requireNonNull(entitiesSupplier, "Entities supplier not set");
            Objects.requireNonNull(ticks, "Ticks not set");
            DebugConfiguration debugging = DebugConfiguration.load(homeDirectory);
            debugging.serverUrl.ifPresent(collarServerURL -> {
                LOGGER.log(Level.INFO, "Debug file has specified an alternate collar server url " + collarServerURL + " that will be used instead of " + this.collarServerURL);
                this.collarServerURL = collarServerURL;
            });
            Supplier<Location> playerPosition = MoreObjects.firstNonNull(this.playerLocation, () -> {
                LOGGER.log(Level.WARNING, "Location features are disabled. Consumer did not provide a player position supplier");
                return Location.UNKNOWN;
            });
            return new CollarConfiguration(playerPosition, sessionSupplier, entitiesSupplier, homeDirectory, debugging, collarServerURL, listener, ticks);
        }
    }
}
