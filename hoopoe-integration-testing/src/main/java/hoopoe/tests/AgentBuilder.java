package hoopoe.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class AgentBuilder {

    private static final String AGENT_JAR_PROTOTYPE_PATH = "/hoopoe-test-agent.jar";
    private static final String AGENT_JAR_PROTOTYPE_NOT_FOUND = "What's wrong with the assembly? Can't find "
            + AGENT_JAR_PROTOTYPE_PATH;

    /**
     * Copies agent jar prototype to {@code destinationFile} and updates it with dynamic configuration and requested
     * plugins / extensions.
     */
    public void createAgentJar(
            File destinationFile,
            Collection<HoopoeIntegrationTest.HoopoeComponent> plugins,
            Collection<HoopoeIntegrationTest.HoopoeComponent> extensions) throws Exception {

        copyAgentPrototypeToDestinationFile(destinationFile);
        String hoopoeConfig = new AgentConfig().getHoopoeConfig(plugins, extensions);

        URI agentJarUri = URI.create("jar:file:" + destinationFile.getAbsolutePath());

        log.info("will write agent jar {}", agentJarUri);
        try (FileSystem agentJarFs = FileSystems.newFileSystem(agentJarUri, new HashMap<>())) {
            Files.copy(
                    new ByteArrayInputStream(hoopoeConfig.getBytes(StandardCharsets.UTF_8)),
                    agentJarFs.getPath("/hoopoe-config.yml")
            );
            log.info("config file is written");

            copyComponents(agentJarFs, plugins);
            copyComponents(agentJarFs, extensions);
        }

        log.info("agent jar is created");
    }

    private void copyComponents(
            FileSystem agentJarFs,
            Collection<HoopoeIntegrationTest.HoopoeComponent> components) throws URISyntaxException, IOException {

        for (HoopoeIntegrationTest.HoopoeComponent component : components) {
            Path componentSource = Paths.get(component.getArchiveUrl().toURI());
            Path componentTarget = agentJarFs.getPath(component.getComponentFile());
            Files.copy(componentSource, componentTarget);

            log.info("{} is written", component);
        }
    }

    private void copyAgentPrototypeToDestinationFile(File destinationFile) throws IOException {
        URL agentJarPrototypeUrl = AgentBuilder.class.getResource(AGENT_JAR_PROTOTYPE_PATH);
        Objects.requireNonNull(agentJarPrototypeUrl, AGENT_JAR_PROTOTYPE_NOT_FOUND);

        try (InputStream agentJarPrototype = agentJarPrototypeUrl.openStream()) {
            Files.copy(agentJarPrototype, destinationFile.toPath());
        }
    }

}
