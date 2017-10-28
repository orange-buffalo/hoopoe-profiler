package hoopoe.tests;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

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

        Map<String, Object> coreRoots = new HashMap<>();
        coreRoots.put("minimum-tracked-invocation-time", 1000000L);
        configRoot.put("core", coreRoots);

        Representer represent = new Representer();
        represent.addClassTag(Long.class, new Tag("!!java.lang.Long"));
        Yaml yaml = new Yaml(represent, new DumperOptions());
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
