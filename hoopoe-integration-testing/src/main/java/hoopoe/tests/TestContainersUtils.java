package hoopoe.tests;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.containers.Container;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestContainersUtils {

    public static void waitForContainerToStop(Container container, long timeoutInMillis) {
        long waitStarted = System.currentTimeMillis();
        while (container.isRunning()) {
            if (System.currentTimeMillis() - waitStarted > timeoutInMillis) {
                throw new IllegalStateException("Container is still running");
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
