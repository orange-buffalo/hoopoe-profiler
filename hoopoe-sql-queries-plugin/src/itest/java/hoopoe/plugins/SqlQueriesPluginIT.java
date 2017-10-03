package hoopoe.plugins;

import hoopoe.api.HoopoeInvocationAttribute;
import hoopoe.api.HoopoeProfiledInvocationRoot;
import hoopoe.api.HoopoeProfiledResult;
import hoopoe.tests.HoopoeIntegrationTest;
import hoopoe.tests.StdoutConsumer;
import hoopoe.tests.TestContainersUtils;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
public class SqlQueriesPluginIT {

    private static final String CONTAINER_APP_PATH = "/opt/app.jar";
    private static final String RESOURCES_APP_PATH = "/itest-app.jar";
    private static final int ENDPOINT_PORT = 8080;
    private static final String DB_NAME = Objects.requireNonNull(
            System.getenv("ITEST_DB"),
            "Integration tests can be executed only against particular database");

    @ClassRule
    public static final Network network = Network.newNetwork();

    @ClassRule
    public static HoopoeIntegrationTest integrationTest = new HoopoeIntegrationTest()
            .withContainer(new GenericContainer(new ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", "/db-images/" + DB_NAME + ".Dockerfile"))
                    .withNetwork(network)
                    .withNetworkAliases("db")
            )
            .withHoopoeContainer(new GenericContainer("openjdk:8-jre")
                    .withNetwork(network)
                    .withExposedPorts(ENDPOINT_PORT)
                    .withClasspathResourceMapping(RESOURCES_APP_PATH, CONTAINER_APP_PATH, BindMode.READ_ONLY)
                    .withCommand(
                            "java",
                            HoopoeIntegrationTest.HOOPOE_AGENT_JMV_ARG,
                            "-jar",
                            "-Dspring.profiles.active=" + DB_NAME,
                            CONTAINER_APP_PATH
                    )
                    .withLogConsumer(new StdoutConsumer())
                    .waitingFor(TestContainersUtils.waitForHttp("/heart-beat", ENDPOINT_PORT)
                            .withStartupTimeout(Duration.ofSeconds(30)))
            )
            .withPlugin(SqlQueriesPluginIT.class.getResource("/sql-plugin.zip"));

    @Test
    public void testStatementExecuteQuery() throws InterruptedException {
        executeProfiledAndAssertQuery(
                "/statement-execute-query",
                "Leela",
                "SELECT last_name FROM emp WHERE first_name = 'Turanga'"
        );
    }

    @Test
    public void testStatementExecute() throws InterruptedException {
        executeProfiledAndAssertQuery(
                "/statement-execute",
                "statement-execute-done",
                "INSERT INTO company (name) VALUES ('Planet Express')"
        );
    }

    @Test
    public void testStatementExecuteUpdate() throws InterruptedException {
        executeProfiledAndAssertQuery(
                "/statement-execute-update",
                "statement-execute-update-done",
                "INSERT INTO company (name) VALUES ('MomCorp')"
        );
    }

    @Test
    public void testStatementExecuteBatch() throws InterruptedException {
        executeProfiledAndAssertQuery(
                "/statement-execute-batch",
                "statement-execute-batch-done",
                "INSERT INTO company (name) VALUES ('Planet Express')",
                "INSERT INTO company (name) VALUES ('MomCorp')"
        );
    }

    @Test
    public void testPreparedStatementExecuteQuery() throws InterruptedException {
        executeProfiledAndAssertQuery(
                "/prepared-statement-execute-query",
                "Fry",
                "SELECT last_name FROM emp WHERE first_name = ?"
        );
    }

    @Test
    public void testPreparedStatementExecute() throws InterruptedException {
        executeProfiledAndAssertQuery(
                "/prepared-statement-execute",
                "prepared-statement-execute-done",
                "INSERT INTO company (name) VALUES (?)"
        );
    }

    @Test
    public void testPreparedStatementExecuteUpdate() throws InterruptedException {
        executeProfiledAndAssertQuery(
                "/prepared-statement-execute-update",
                "prepared-statement-execute-update-done",
                "DELETE FROM company"
        );
    }

    @Test
    public void testPreparedStatementExecuteBatch() throws InterruptedException {
        executeProfiledAndAssertQuery(
                "/prepared-statement-execute-batch",
                "prepared-statement-execute-batch-done",
                "UPDATE company SET name = ? WHERE name = ?"
        );
    }

    @Test
    public void testCallableStatementExecute() throws InterruptedException {
        executeProfiledAndAssertQuery(
                "/callable-statement-execute",
                "callable-statement-execute-done",
                "{call get_emps()}"
        );
    }

    private void executeProfiledAndAssertQuery(String appPath, String appResponse, String... expectedQueries) {
        HoopoeProfiledResult profiledResult = integrationTest.executeProfiled(() -> {
            String sqlQueryResponse = integrationTest.httpEndpoint(appPath).executeGetForString();

            assertThat("App in container should return proper query result", sqlQueryResponse, equalTo(appResponse));

        });

        assertThat("Framework should return not-null result any time", profiledResult, notNullValue());

        HoopoeProfiledInvocationRoot sqlThreadInvocation = profiledResult.getInvocations().stream()
                .filter(invocation -> "sql-thread".equals(invocation.getThreadName()))
                .findAny()
                .orElse(null);
        assertThat("Invocation for a thread with sql queries should be recorded", sqlThreadInvocation, notNullValue());

        Collection<HoopoeInvocationAttribute> attributes = sqlThreadInvocation.getInvocation().flattened()
                .filter(invocation -> !invocation.getAttributes().isEmpty())
                .flatMap(invocation -> invocation.getAttributes().stream())
                .collect(Collectors.toList());

        Set<String> actualAttributeName = attributes.stream()
                .map(HoopoeInvocationAttribute::getName)
                .collect(Collectors.toSet());
        assertThat("Proper attribute name must be recorded", actualAttributeName, containsInAnyOrder("SQL Query"));

        Set<String> actualQueries = attributes.stream()
                .map(HoopoeInvocationAttribute::getDetails)
                .collect(Collectors.toSet());
        // we cannot use containsInAnyOrder as additional queries might be executed, like
        // "select @@session.tx_read_only" for mysql
        for (String expectedQuery : expectedQueries) {
            assertThat("Query " + expectedQuery + " must be recorded", actualQueries.contains(expectedQuery));
        }

        Set<Boolean> actualContributingTime = attributes.stream()
                .map(HoopoeInvocationAttribute::isContributingTime)
                .collect(Collectors.toSet());
        assertThat("Proper contributing time flag name must be recorded",
                actualContributingTime, containsInAnyOrder(true));
    }
}
