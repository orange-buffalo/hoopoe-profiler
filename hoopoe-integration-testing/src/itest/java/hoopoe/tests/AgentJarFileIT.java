package hoopoe.tests;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
public class AgentJarFileIT {

    private static final GenericContainer container = new GenericContainer("ubuntu:xenial")
            .withCommand("tail -f /dev/null");

    @ClassRule
    public static HoopoeIntegrationTest integrationTest = new HoopoeIntegrationTest().withHoopoeContainer(container);

    @Test
    public void verifyFileExists() throws InterruptedException, IOException {
        Container.ExecResult printEnvResult = container.execInContainer(
                "ls", HoopoeIntegrationTest.HOOPOE_AGENT_JAR_CONTAINER_PATH);

        String containerOutput = printEnvResult.getStdout();

        assertThat("We expect container to output something",
                containerOutput, notNullValue());

        assertThat("We expect container to output some specific thing",
                containerOutput.trim(), equalTo(HoopoeIntegrationTest.HOOPOE_AGENT_JAR_CONTAINER_PATH));
    }

}
