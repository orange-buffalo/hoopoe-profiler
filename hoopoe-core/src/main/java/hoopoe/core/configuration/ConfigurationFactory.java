package hoopoe.core.configuration;

import hoopoe.core.Environment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Creates all the necessary configuration engine components and wire them together.
 * Produces {@link Configuration} object ready to be used in profiler.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigurationFactory {

    public static Configuration createConfiguration(Environment environment) {
        MapsMerger mapsMerger = new MapsMerger();
        ConfigurationBeanPropertiesReader configurationBeanPropertiesReader = new ConfigurationBeanPropertiesReader();
        YamlDocumentsReader yamlDocumentsReader = new YamlDocumentsReader();
        ConfigurationDataReader configurationDataReader =
                new ConfigurationDataReader(environment, yamlDocumentsReader, mapsMerger);
        return new Configuration(configurationDataReader, configurationBeanPropertiesReader);
    }
}
