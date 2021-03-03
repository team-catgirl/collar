package team.catgirl.collar.tests.junit;

import com.google.common.io.Files;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.CollarConfiguration;
import team.catgirl.collar.client.minecraft.Ticks;
import team.catgirl.collar.security.mojang.MinecraftSession;

import java.util.UUID;
import java.util.function.Supplier;

public final class CollarClientRule implements TestRule {

    public Collar collar;
    private Ticks ticks;

    private final CollarConfiguration.Builder builder;
    private final Thread thread = new Thread(() -> {
        collar.connect();
        do {
            try {
                if (collar.getState() == Collar.State.CONNECTED) {
                    ticks.onTick();
                }
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                collar.disconnect();
            }
        } while (collar.getState() != Collar.State.DISCONNECTED);
    }, "Collar Client Test Loop");

    public CollarClientRule(UUID playerId, CollarConfiguration.Builder builder) {
        this(playerId, builder, MinecraftSession.noJang(playerId, "cuteplayer", "hypixel.net"));
    }
    public CollarClientRule(UUID playerId, CollarConfiguration.Builder builder, MinecraftSession session) {
        this.ticks = new Ticks();
        this.builder = builder.withCollarServer("http://localhost:3001")
                .withHomeDirectory(Files.createTempDir())
                .withSession(() -> session)
                .withTicks(ticks);
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
