package team.catgirl.collar.tests.junit;

import com.mongodb.client.MongoDatabase;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import spark.Spark;
import team.catgirl.collar.server.Main;
import team.catgirl.collar.server.Services;
import team.catgirl.collar.server.configuration.Configuration;
import team.catgirl.collar.server.mongo.Mongo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.function.Consumer;

public final class CollarServerRule implements TestRule {

    private final Consumer<Services> setupState;
    public Main.Server server;

    public CollarServerRule(Consumer<Services> setupState) {
        this.setupState = setupState;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Mongo.getTestingDatabase().drop();
        MongoDatabase db = Mongo.getTestingDatabase();
        server = new Main.Server(Configuration.testConfiguration(db));
        return new Statement() {
            @Override public void evaluate() throws Throwable {
                server.start(setupState);
                try {
                    base.evaluate();
                } finally {
                    Spark.stop();
                    db.drop();
                }
            }
        };
    }

    public boolean isServerStarted() {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:3001/api/discover"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            return false;
        }
        return response.statusCode() == 200;
    }
}
