package hoopoe.tests;

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

public class HoopoeIntegrationTest implements TestRule {

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
    private int integrationTestExtensionExposedPort = 9271;
    private int hoopoeComponentsCount = 0;

    public HoopoeIntegrationTest withPlugin(URL pluginArchiveUrl) {
        return withPlugin(pluginArchiveUrl, (plugin) -> {
        });
    }

    public HoopoeIntegrationTest withPlugin(URL pluginArchiveUrl, Consumer<HoopoeComponent> pluginConfig) {
        HoopoeComponent plugin = new HoopoeComponent(pluginArchiveUrl);
        pluginConfig.accept(plugin);
        plugins.add(plugin);
        return this;
    }

    public HoopoeIntegrationTest withExtension(URL extensionArchiveUrl) {
        return withExtension(extensionArchiveUrl, (plugin) -> {
        });
    }

    public HoopoeIntegrationTest withExtension(URL extensionArchiveUrl, Consumer<HoopoeComponent> extensionConfig) {
        HoopoeComponent extension = new HoopoeComponent(extensionArchiveUrl);
        extensionConfig.accept(extension);
        extensions.add(extension);
        return this;
    }

    public HoopoeIntegrationTest withIntegrationTestExtensionPort(int integrationTestExtensionPort) {
        this.integrationTestExtensionPort = integrationTestExtensionPort;
        return this;
    }

    public HoopoeIntegrationTest withIntegrationTestExtensionExposedPort(int integrationTestExtensionExposedPort) {
        this.integrationTestExtensionExposedPort = integrationTestExtensionExposedPort;
        return this;
    }

    private void prepareContainers() throws Exception {
        enableIntegrationTestExtension();
        File agentJar = buildAgentJar();


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
