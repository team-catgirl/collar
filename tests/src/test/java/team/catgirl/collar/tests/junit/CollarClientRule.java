package team.catgirl.collar.tests.junit;

import com.google.common.io.Files;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.net.ConnectException;
import java.util.UUID;

public final class CollarClientRule implements TestRule {

    public Collar collar;

    public final CollarConfiguration.Builder builder;
    public final Thread thread = new Thread(() -> {
        collar.connect();
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                collar.disconnect();
            }
        } while (collar.getState() != Collar.State.DISCONNECTED);
    }, "Collar Client Test Loop");

    public CollarClientRule(UUID playerId, CollarConfiguration.Builder builder) {
        this.builder = builder.withCollarServer("http://localhost:3001")
                .withHomeDirectory(Files.createTempDir())
                .withNoJangAuthentication(playerId, "hypixel.net");
    }

    public CollarClientRule(UUID playerId, CollarConfiguration.Builder builder, MinecraftSession sess) {
        this.builder = builder.withCollarServer("http://localhost:3001")
                .withHomeDirectory(Files.createTempDir())
                .withMojangAuthentication(() -> sess);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                collar = Collar.create(builder.build());
                thread.start();
                try {
                    base.evaluate();
                } finally {
                    thread.interrupt();
                }
            }
        };
    }
}
