package hoopoe.tests;

import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

@Slf4j
public class AgentJarEnvIT {

    private static final GenericContainer container = new GenericContainer("ubuntu:xenial")
            .withCommand("printenv " + HoopoeIntegrationTest.HOOPOE_AGENT_ENV);

    @ClassRule
    public static HoopoeIntegrationTest integrationTest = new HoopoeIntegrationTest().withHoopoeContainer(container);

    @Test
    public void verifyEnvVariableIsAvailable() throws InterruptedException {
        TestsCommons.waitForContainerAndAssertOutput(container, HoopoeIntegrationTest.HOOPOE_AGENT_JAR_CONTAINER_PATH);
    }

}
