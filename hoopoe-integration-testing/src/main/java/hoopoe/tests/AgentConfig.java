package hoopoe.tests;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

class AgentConfig {

    /**
     * Generates YAML data for Hoopoe Configuration File - adds provided plugins and extensions with their configs.
     */
    public String getHoopoeConfig(
            Collection<HoopoeIntegrationTest.HoopoeComponent> plugins,
            Collection<HoopoeIntegrationTest.HoopoeComponent> extensions) {

        Map<String, Object> configRoot = new HashMap<>();

        Map<String, Object> pluginsRoot = new HashMap<>();
        writeComponents(pluginsRoot, plugins);
        configRoot.put("plugins", pluginsRoot);

        Map<String, Object> extensionsRoot = new HashMap<>();
        writeComponents(extensionsRoot, extensions);
        configRoot.put("extensions", extensionsRoot);

        Yaml yaml = new Yaml();
        return yaml.dump(configRoot);
    }

    private void writeComponents(
            Map<String, Object> root,
            Collection<HoopoeIntegrationTest.HoopoeComponent> components) {

        for (HoopoeIntegrationTest.HoopoeComponent component : components) {
            Map<String, Object> componentConfig = new HashMap<>();
            componentConfig.put("enabled", true);
            componentConfig.put("path", "classpath:" + component.getComponentFile());
            componentConfig.putAll(component.getProperties());

            root.put(component.getComponentId(), componentConfig);
        }
    }

}
