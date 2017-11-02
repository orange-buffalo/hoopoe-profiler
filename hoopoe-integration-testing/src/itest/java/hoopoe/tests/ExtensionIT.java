package hoopoe.tests;

import hoopoe.api.HoopoeProfiledResult;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@Slf4j
public class ExtensionIT {

    private static final GenericContainer<?> container =
            new GenericContainer<>(new ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", "/tomcat8.Dockerfile"))

                    .withExposedPorts(8080)
                    .waitingFor(TestContainersUtils.waitForHttp("/", 8080));

    @ClassRule
    public static HoopoeIntegrationTest integrationTest = new HoopoeIntegrationTest().withHoopoeContainer(container);

    @Test
    public void testProfiledResultIsNotNullForNoExecutions() throws InterruptedException {
        container.followOutput(new Slf4jLogConsumer(log));
        HoopoeProfiledResult profiledResult = integrationTest.executeProfiled(() -> {
        });

        assertThat("Framework should return not-null result any time", profiledResult, notNullValue());
    }

    @Test
    public void testProfiledResultHasDataForSimpleExecution() throws InterruptedException {
        container.followOutput(new Slf4jLogConsumer(log));
        HoopoeProfiledResult profiledResult = integrationTest.executeProfiled(() -> {
            try {
                HttpClients.createDefault().execute(
                        new HttpGet("http://" + container.getContainerIpAddress()
                                + ":" + container.getMappedPort(8080)));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat("Framework should return not-null result any time", profiledResult, notNullValue());
        assertThat("There should be some data when server is requested",
                profiledResult.getInvocations(), notNullValue());
        assertThat("There should be some data when server is requested",
                profiledResult.getInvocations().size(), greaterThan(0));
    }

}
