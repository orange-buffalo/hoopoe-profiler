package hoopoe.core.configuration;

import hoopoe.core.HoopoeException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * Effectively is a wrapper on a backing map for convenient read of configuration values.
 */
public class ConfigurationData {

    public static final String CORE_NAMESPACE = "core";
    public static final String PROFILE_KEY = "profile";

    public static final String PLUGINS_NAMESPACE = "plugins";
    private static final String EXTENSIONS_NAMESPACE = "extensions";
    public static final String ENABLED_KEY = "enabled";
    public static final String PATH_KEY = "path";

    private final Map<String, Object> configurationData;

    public ConfigurationData(Map<String, Object> configurationData) {
        this.configurationData = configurationData;
    }

    /**
     * Gets all plugins, enabled in current configuration.
     * @return enabled plugins info or empty collection, if none is enabled.
     */
    public Collection<EnabledComponentData> getEnabledPlugins() {
        return getEnabledComponentsData(PLUGINS_NAMESPACE);
    }

    /**
     * Gets all extensions, enabled in current configuration.
     * @return enabled extensions info or empty collection, if none is enabled.
     */
    public Collection<EnabledComponentData> getEnabledExtensions() {
        return getEnabledComponentsData(EXTENSIONS_NAMESPACE);
    }

    private Collection<EnabledComponentData> getEnabledComponentsData(String componentsNamespace) {
        Map<String, Object> data = getConfigurationValue(
                configurationData,
                Path.of(componentsNamespace),
                Map.class,
                new HashMap<String, Object>());

        return data.keySet().stream()
                .filter(componentId -> getConfigurationValue(
                        configurationData,
                        Path.of(componentsNamespace, componentId, ENABLED_KEY),
                        Boolean.class,
                        Boolean.FALSE)
                )
                .map(componentId -> {
                    String binariesPath = getConfigurationValue(
                            configurationData,
                            Path.of(componentsNamespace, componentId, PATH_KEY),
                            String.class,
                            null);
                    if (StringUtils.isBlank(binariesPath)) {
                        throw new HoopoeException("Plugin " + componentId + " has no path defined in configuration");
                    }
                    return new EnabledComponentData(componentId, binariesPath);
                })
                .collect(Collectors.toList());
    }

    /**
     * Reads the configuration value by provided path.
     * If value is {@code null} or node is missing in the middle of the path, returns {@code defaultValue}.
     * If any of the intermediate nodes is of any other type but map, fails.
     * If {@code expectedValueType} is not assignable from actual value type, fails.
     *
     * @param path              dot-separated path to traverse by.
     * @param expectedValueType type of the value to be expected and validated.
     * @param defaultValue      value to be returned if path does not have the value.
     * @param <T>               type of the value.
     *
     * @return configuration value, default value or throws an exception.
     */
    private <T> T getConfigurationValue(
            Map<String, Object> root,
            Path path,
            Class<T> expectedValueType,
            T defaultValue) {

        Object value = readPathValue(root, path);

        if (value == null) {
            return defaultValue;

        } else {
            Class actualValueType = value.getClass();
            if (!expectedValueType.isAssignableFrom(actualValueType)) {
                throw new HoopoeException("Expected type " + expectedValueType + " does not match actual type " +
                        actualValueType + " for value of " + path);
            }
            return (T) value;
        }
    }

    private Object readPathValue(
            Map<String, Object> root,
            Path path) {

        Map<String, Object> nextElement = root;
        Object value = null;
        for (int pathElementPos = 0; pathElementPos < path.pathElements.length; pathElementPos++) {
            String pathElement = path.pathElements[pathElementPos];
            value = nextElement.get(pathElement);
            if (value == null) {
                break;

            } else if (value instanceof Map) {
                nextElement = (Map<String, Object>) value;

            } else if (pathElementPos < path.pathElements.length - 1) {
                throw new HoopoeException("Cannot read " + path + " as it is terminated on " + pathElement);
            }
        }
        return value;
    }

    private static class Path {
        private String[] pathElements;

        static Path of(String... pathElements) {
            Path path = new Path();
            path.pathElements = pathElements;
            return path;
        }

        @Override
        public String toString() {
            return Stream.of(pathElements).collect(Collectors.joining("."));
        }
    }

}
