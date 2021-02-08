package team.catgirl.collar.tests.junit;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class CollarAssert {

    public static void waitForCondition(String name, Supplier<Boolean> condition, long waitFor, TimeUnit timeUnit) throws InterruptedException {
        long future = timeUnit.toMillis(waitFor) + System.currentTimeMillis();
        while (System.currentTimeMillis() < future) {
            Boolean aBoolean = condition.get();
            if (aBoolean != null && aBoolean) {
                return;
            }
            Thread.sleep(200);
        }
        Assert.fail("waitForCondition '" + name + "' failed");
    }

    public static void waitForCondition(String name, Supplier<Boolean> condition) throws InterruptedException {
        waitForCondition(name, condition, 30, TimeUnit.SECONDS);
    }

    public CollarAssert() {}
}
