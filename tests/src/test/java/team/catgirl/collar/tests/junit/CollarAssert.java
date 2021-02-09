package team.catgirl.collar.tests.junit;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class CollarAssert {

    public static void waitForCondition(String name, Supplier<Boolean> condition, long waitFor, TimeUnit timeUnit) {
        long future = timeUnit.toMillis(waitFor) + System.currentTimeMillis();
        while (System.currentTimeMillis() < future) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Assert.fail("interrupted sleep");
            }
            Boolean aBoolean = condition.get();
            if (aBoolean != null && aBoolean) {
                return;
            }
        }
        Assert.fail("waitForCondition '" + name + "' failed");
    }

    public static void waitForCondition(String name, Supplier<Boolean> condition) {
        waitForCondition(name, condition, 15, TimeUnit.SECONDS);
    }

    public CollarAssert() {}
}
