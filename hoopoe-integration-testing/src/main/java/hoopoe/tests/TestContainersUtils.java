package hoopoe.tests;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.wait.HttpWaitStrategy;

/**
 * Various utility methods we miss in Test Containers.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestContainersUtils {

    /**
     * Waits until container is stopped (i.e. cmd in container finishes its execution).
     *
     * @param container       container to wait for.
     * @param timeoutInMillis if container is running after this timeout, exception is thrown.
     */
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

    /**
     * Extension of {@link org.testcontainers.containers.wait.Wait#forHttp(String)} that allows to specify the port to
     * wait for (default implementation just takes first exposed port)
     *
     * @param path        see {@link HttpWaitStrategy#forPath(String)}.
     * @param exposedPort exposed port to listen to.
     *
     * @return new strategy that will wait for provided path and port. Can be configured further as any other {@link
     * HttpWaitStrategy}.
     */
    public static HttpWaitStrategy waitForHttp(String path, Integer exposedPort) {
        return new HttpWaitStrategy() {
            @Override
            protected Integer getLivenessCheckPort() {
                return container.getMappedPort(exposedPort);
            }
        }.forPath(path);
    }

}
