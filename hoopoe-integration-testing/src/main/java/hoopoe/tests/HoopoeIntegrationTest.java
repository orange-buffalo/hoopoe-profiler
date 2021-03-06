package hoopoe.tests;

import hoopoe.api.HoopoeProfiledResult;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

/**
 * JUnit rule to be used for integration testing of Hoopoe plugins and extensions.
 * <p>
 * Works seamlessly with Test Containers and deploys Hoopoe agent with all enabled plugins / extensions to the target
 * container. Provides API to access results of profiling in container.
 * <p>
 * See https://orange-buffalo.github.io/hoopoe-profiler/dev-guide/integration-testing/
 */
public class HoopoeIntegrationTest implements TestRule {

    public static final String HOOPOE_AGENT_ENV = "HOOPOE_AGENT";
    public static final String HOOPOE_AGENT_JAR_CONTAINER_PATH = "/opt/hoopoe-agent.jar";
    public static final String HOOPOE_AGENT_JMV_ARG = "-javaagent:" + HOOPOE_AGENT_JAR_CONTAINER_PATH;

    private static final String HOOPOE_AGENT_JAR_NAME = "hoopoe-test-agent.jar";
    private static final String INTEGRATION_TESTING_EXTENSION_PATH = "/hoopoe-integration-testing.zip";
    private static final String INTEGRATION_TESTING_EXTENSION_NOT_FOUND = "Something terrible happened to assembly, "
            + "cannot find " + INTEGRATION_TESTING_EXTENSION_PATH;

    private TemporaryFolder temporaryFolderRule = new TemporaryFolder();

    @Delegate(types = TestRule.class)
    private RuleChain ruleChain = RuleChain.outerRule(temporaryFolderRule)
            .around((base, description) -> new HoopoeIntegrationTestStatement(base));

    private List<HoopoeComponent> plugins = new ArrayList<>();
    private List<HoopoeComponent> extensions = new ArrayList<>();
    private int integrationTestExtensionPort = 9271;
    private int hoopoeComponentsCount = 0;
    private GenericContainer<?> hoopoeContainer;

    /**
     * Adds plugin to be deployed to target container within Hoopoe agent.
     *
     * @param pluginArchiveUrl URL to assembled plugin.
     *
     * @return this rule to be configured further.
     */
    public HoopoeIntegrationTest withPlugin(URL pluginArchiveUrl) {
        return withPlugin(pluginArchiveUrl, (plugin) -> {
        });
    }

    /**
     * Adds plugin to be deployed to target container within Hoopoe agent. Uses {@code pluginConfig} to provide any
     * configuration to this plugin (via hoopoe config file).
     *
     * @param pluginArchiveUrl URL to assembled plugin.
     * @param pluginConfig     will be called to provide plugin configuration.
     *
     * @return this rule to be configured further.
     */
    public HoopoeIntegrationTest withPlugin(URL pluginArchiveUrl, Consumer<HoopoeComponent> pluginConfig) {
        HoopoeComponent plugin = new HoopoeComponent(pluginArchiveUrl);
        pluginConfig.accept(plugin);
        plugins.add(plugin);
        return this;
    }

    /**
     * Adds extension to be deployed to target container within Hoopoe agent.
     *
     * @param extensionArchiveUrl URL to assembled extension.
     *
     * @return this rule to be configured further.
     */
    public HoopoeIntegrationTest withExtension(URL extensionArchiveUrl) {
        return withExtension(extensionArchiveUrl, (plugin) -> {
        });
    }

    /**
     * Adds extension to be deployed to target container within Hoopoe agent. Uses {@code extensionConfig} to provide
     * any configuration to this extension (via hoopoe config file).
     *
     * @param extensionArchiveUrl URL to assembled extension.
     * @param extensionConfig     will be called to provide extension configuration.
     *
     * @return this rule to be configured further.
     */
    public HoopoeIntegrationTest withExtension(URL extensionArchiveUrl, Consumer<HoopoeComponent> extensionConfig) {
        HoopoeComponent extension = new HoopoeComponent(extensionArchiveUrl);
        extensionConfig.accept(extension);
        extensions.add(extension);
        return this;
    }

    /**
     * Changes the port to listen by technical extension this rule communicates with.
     * <p>
     * Should be used in case default port (9271) is used by anything deployed in user's containers.
     *
     * @param integrationTestExtensionPort port to listen to.
     *
     * @return this rule to be configured further.
     */
    public HoopoeIntegrationTest withIntegrationTestExtensionPort(int integrationTestExtensionPort) {
        this.integrationTestExtensionPort = integrationTestExtensionPort;
        return this;
    }

    /**
     * Defines container where Hoopoe agent will be deployed.
     *
     * @param container container with Hoopoe agent; {@code HOOPOE_AGENT} environment variable will be available in this
     *                  container pointing to the agent jar.
     *
     * @return this rule to be configured further.
     */
    public HoopoeIntegrationTest withHoopoeContainer(GenericContainer container) {
        this.hoopoeContainer = container;
        this.ruleChain = this.ruleChain.around(container);
        return this;
    }

    /**
     * Adds containers to be created and started during test execution.
     *
     * @param container container to add.
     *
     * @return this rule to be configured further.
     */
    public HoopoeIntegrationTest withContainer(GenericContainer container) {
        this.ruleChain = this.ruleChain.around(container);
        return this;
    }

    /**
     * Executes {@code code} with profiler enabled. Typically, user will call HTTP endpoint in container to trigger
     * target code execution.
     *
     * @param code code to be executed while profiler is enabled.
     *
     * @return profiled result; never {@code null}.
     */
    public HoopoeProfiledResult executeProfiled(Runnable code) {
        ExtensionConnector extensionConnector = new ExtensionConnector(
                hoopoeContainer.getContainerIpAddress(),
                hoopoeContainer.getMappedPort(integrationTestExtensionPort));
        extensionConnector.startProfiling();
        code.run();
        return extensionConnector.stopProfiling();
    }

    /**
     * Creates an {@link HttpEndpoint} for provided {@code path}, using the first exposed port by the container provided
     * to {@link #withContainer(GenericContainer)} method . If no ports are exposed, throws an exception.
     * <p>
     * If container exposes multiple ports, {@link #httpEndpoint(int, String)} should be used instead.
     *
     * @param path endpoint path.
     *
     * @return new instance of endpoint.
     */
    public HttpEndpoint httpEndpoint(String path) {
        int userPort = hoopoeContainer.getExposedPorts().stream()
                .filter(port -> port != integrationTestExtensionPort)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No ports are exposed, cannot create endpoint"));
        return httpEndpoint(userPort, path);
    }

    /**
     * Creates an {@link HttpEndpoint} for provided {@code path} and {@code port}, using the container provided to
     * {@link #withContainer(GenericContainer)} method.
     *
     * @param exposedPort exposed port to connect to.
     * @param path        endpoint path.
     *
     * @return new instance of endpoint.
     */
    public HttpEndpoint httpEndpoint(int exposedPort, String path) {
        return HttpEndpoint.forContainer(hoopoeContainer, exposedPort, path);
    }

    private void prepareContainers() throws Exception {
        Objects.requireNonNull(hoopoeContainer,
                "withHoopoeContainer method must be called when building HoopoeIntegrationTest");

        enableIntegrationTestExtension();
        File agentJar = buildAgentJar();

        hoopoeContainer.addExposedPort(integrationTestExtensionPort);
        hoopoeContainer.addFileSystemBind(
                agentJar.getAbsolutePath(), HOOPOE_AGENT_JAR_CONTAINER_PATH, BindMode.READ_ONLY);
        hoopoeContainer.addEnv(HOOPOE_AGENT_ENV, HOOPOE_AGENT_JAR_CONTAINER_PATH);
    }

    private File buildAgentJar() throws Exception {
        File agentFile = new File(temporaryFolderRule.getRoot(), HOOPOE_AGENT_JAR_NAME);
        AgentBuilder agentBuilder = new AgentBuilder();
        agentBuilder.createAgentJar(agentFile, plugins, extensions);
        return agentFile;
    }

    private void enableIntegrationTestExtension() {
        URL extensionUrl = HoopoeIntegrationTest.class.getResource(INTEGRATION_TESTING_EXTENSION_PATH);
        Objects.requireNonNull(extensionUrl, INTEGRATION_TESTING_EXTENSION_NOT_FOUND);
        withExtension(extensionUrl, (extension) -> extension.withProperty("port", integrationTestExtensionPort));
    }

    @ToString(of = "archiveUrl")
    public class HoopoeComponent {

        @Getter(AccessLevel.PACKAGE)
        private URL archiveUrl;

        @Getter(AccessLevel.PACKAGE)
        private String componentId;

        @Getter(AccessLevel.PACKAGE)
        private String componentFile;

        @Getter(AccessLevel.PACKAGE)
        private Map<String, Object> properties = new HashMap<>();

        HoopoeComponent(URL archiveUrl) {
            this.archiveUrl = archiveUrl;
            this.componentId = "component" + hoopoeComponentsCount;
            this.componentFile = this.componentId + ".zip";
            hoopoeComponentsCount++;
        }

        public HoopoeComponent withProperty(String key, Object value) {
            properties.put(key, value);
            return this;
        }
    }

    @AllArgsConstructor
    private class HoopoeIntegrationTestStatement extends Statement {
        private Statement base;

        @Override
        public void evaluate() throws Throwable {
            prepareContainers();
            base.evaluate();
        }
    }
}
