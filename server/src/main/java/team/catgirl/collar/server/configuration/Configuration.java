package team.catgirl.collar.server.configuration;

import com.mongodb.client.MongoDatabase;
import okhttp3.OkHttpClient;
import team.catgirl.collar.server.http.AppUrlProvider;
import team.catgirl.collar.server.http.CollarWebAppUrlProvider;
import team.catgirl.collar.server.http.DefaultAppUrlProvider;
import team.catgirl.collar.server.mail.Email;
import team.catgirl.collar.server.mail.LocalEmail;
import team.catgirl.collar.server.mail.MailGunEmail;
import team.catgirl.collar.server.mongo.Mongo;
import team.catgirl.collar.server.security.hashing.PasswordHashing;
import team.catgirl.collar.server.security.mojang.MinecraftSessionVerifier;
import team.catgirl.collar.server.security.mojang.MojangMinecraftSessionVerifier;
import team.catgirl.collar.server.security.mojang.NojangMinecraftSessionVerifier;
import team.catgirl.collar.server.services.authentication.TokenCrypter;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Configuration {
    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    public final MongoDatabase database;
    public final AppUrlProvider appUrlProvider;
    public final TokenCrypter tokenCrypter;
    public final PasswordHashing passwordHashing;
    public final MinecraftSessionVerifier minecraftSessionVerifier;
    public final String corsOrigin;
    public final boolean enableWeb;
    public final int httpPort;
    public final Email email;
    public final OkHttpClient http;

    public Configuration(MongoDatabase database,
                         AppUrlProvider appUrlProvider,
                         TokenCrypter tokenCrypter,
                         PasswordHashing passwordHashing,
                         MinecraftSessionVerifier minecraftSessionVerifier,
                         String corsOrigin,
                         boolean enableWeb,
                         int httpPort,
                         Email email,
                         OkHttpClient http
    ) {
        this.database = database;
        this.appUrlProvider = appUrlProvider;
        this.tokenCrypter = tokenCrypter;
        this.passwordHashing = passwordHashing;
        this.minecraftSessionVerifier = minecraftSessionVerifier;
        this.corsOrigin = corsOrigin;
        this.enableWeb = enableWeb;
        this.httpPort = httpPort;
        this.email = email;
        this.http = http;
        LOGGER.log(Level.INFO, "Using Email type " + email.getClass().getSimpleName());
    }

    public static Configuration fromEnvironment() {
        String baseUrl = System.getenv("COLLAR_APP_URL");
        if (baseUrl == null) {
            throw new IllegalStateException("COLLAR_APP_URL not set");
        }
        String crypterPassword = System.getenv("COLLAR_CRYPTER_PASSWORD");
        if (crypterPassword == null) {
            throw new IllegalStateException("COLLAR_CRYPTER_PASSWORD not set");
        }
        String passwordSalt = System.getenv("COLLAR_PASSWORD_SALT");
        if (passwordSalt == null) {
            throw new IllegalStateException("COLLAR_PASSWORD_SALT not set");
        }
        String verifyMojangSessions = System.getenv("COLLAR_VERIFY_MOJANG_SESSIONS");
        if (verifyMojangSessions == null) {
            throw new IllegalStateException("COLLAR_VERIFY_MOJANG_SESSIONS not set");
        }
        boolean useMojang = Boolean.parseBoolean(verifyMojangSessions);
        boolean enableWeb = Boolean.parseBoolean(System.getenv("COLLAR_ENABLED_WEB"));
        String mailgunDomain = System.getenv("MAILGUN_DOMAIN");
        if (mailgunDomain == null) {
            throw new IllegalStateException("MAILGUN_DOMAIN not set");
        }
        String mailgunApiKey = System.getenv("MAILGUN_API_KEY");
        if (mailgunApiKey == null) {
            throw new IllegalStateException("MAILGUN_API_KEY not set");
        }
        AppUrlProvider appUrlProvider = new CollarWebAppUrlProvider(baseUrl);
        OkHttpClient http = new OkHttpClient();
        return new Configuration(
                Mongo.database(),
                appUrlProvider,
                new TokenCrypter(crypterPassword),
                new PasswordHashing(passwordSalt),
                useMojang ? new MojangMinecraftSessionVerifier(http, "https://sessionserver.mojang.com/") : new NojangMinecraftSessionVerifier(),
                appUrlProvider.homeUrl(),
                enableWeb,
                httpPort(),
                new MailGunEmail(http, appUrlProvider, mailgunDomain, mailgunApiKey),
                http
        );
    }

    public static Configuration defaultConfiguration() {
        LOGGER.log(Level.SEVERE, "Starting in insecure development mode. Do not use in production.");
        DefaultAppUrlProvider appUrlProvider = new DefaultAppUrlProvider("http://localhost:3000");
        return new Configuration(
                Mongo.database("mongodb://localhost/collar-dev"),
                appUrlProvider,
                new TokenCrypter("insecureTokenCrypterPassword"),
                new PasswordHashing("VSZL*bR8-=r]r5P_"),
                new NojangMinecraftSessionVerifier(),
                "*",
                true,
                httpPort(),
                new LocalEmail(appUrlProvider),
                new OkHttpClient());
    }

    public static Configuration testConfiguration(MongoDatabase db, MinecraftSessionVerifier sessionVerifier) {
        LOGGER.log(Level.SEVERE, "Starting in insecure testing mode. Do not use in production.");
        DefaultAppUrlProvider appUrlProvider = new DefaultAppUrlProvider("http://localhost:3001");
        return new Configuration(
                db,
                appUrlProvider,
                new TokenCrypter("insecureTokenCrypterPassword"),
                new PasswordHashing("VSZL*bR8-=r]r5P_"),
                sessionVerifier,
                "*",
                false,
                3001,
                new LocalEmail(appUrlProvider),
                new OkHttpClient());
    }

    private static int httpPort() {
        String portValue = System.getenv("PORT");
        return portValue != null ? Integer.parseInt(portValue) : 4000;
    }
}
