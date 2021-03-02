package team.catgirl.collar.tests.junit;

import com.mongodb.client.MongoDatabase;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import spark.Spark;
import team.catgirl.collar.server.Services;
import team.catgirl.collar.server.WebServer;
import team.catgirl.collar.server.configuration.Configuration;
import team.catgirl.collar.server.mongo.Mongo;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class CollarServerRule implements TestRule {

    private static final OkHttpClient http = new OkHttpClient();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Consumer<Services> setupState;
    private Thread serverThread;
    public WebServer webServer;

    public CollarServerRule(Consumer<Services> setupState) {
        this.setupState = setupState;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Mongo.getTestingDatabase().drop();
        MongoDatabase db = Mongo.getTestingDatabase();
        return new Statement() {
            @Override public void evaluate() throws Throwable {
                stopServer();
                serverThread = new Thread(() -> {
                    webServer = new WebServer(Configuration.testConfiguration(db));
                    webServer.start(services -> {
                        setupState.accept(services);
                        started.set(true);
                    });
                    while (true) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {}
                    }
                });
                serverThread.start();
                while (!started.get()) {
                    Thread.sleep(500);
                }
                try {
                    base.evaluate();
                } finally {
                    serverThread.interrupt();
                    stopServer();
                    db.drop();
                }
            }
        };
    }

    private void stopServer() {
        Spark.stop();
        Spark.awaitStop();
    }

    public boolean isServerStarted() {
        Request request = new Request.Builder()
                .url("http://localhost:3001/api/discover")
                .build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 200;
        } catch (IOException e) {
            return false;
        }
    }
}
