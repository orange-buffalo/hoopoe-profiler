package hoopoe.core.configuration;

import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.core.HoopoeException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * Effectively is a wrapper on a backing map for convenient read of configuration values.
 */
class ConfigurationData {

    public static final String CORE_NAMESPACE = "core";
    public static final String PROFILE_KEY = "profile";
    public static final String MINIMUM_TRACKED_INVOCATION_TIME = "minimumTrackedInvocationTime";
    public static final String INCLUDED_CLASSES_PATTERNS = "includedClassesPatterns";
    public static final String EXCLUDED_CLASSES_PATTERNS = "excludedClassesPatterns";

    public static final String PLUGINS_NAMESPACE = "plugins";
    public static final String ENABLED_KEY = "enabled";
    public static final String PATH_KEY = "path";
    public static final String EXTENSIONS_NAMESPACE = "extensions";

    private final Map<String, Object> configurationData;

    public ConfigurationData(Map<String, Object> configurationData) {
        this.configurationData = configurationData;
    }

    /**
     * See {@link HoopoeConfiguration#getMinimumTrackedInvocationTimeInNs()}
     */
    public long getMinimumTrackedInvocationTimeInNs() {
        return getConfigurationValue(configurationData,
                Path.of(CORE_NAMESPACE, MINIMUM_TRACKED_INVOCATION_TIME),
                Long.class,
                0L);
    }

    /**
     * See {@link HoopoeConfiguration#getIncludedClassesPatterns()}
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getIncludedClassesPatterns() {
        return getConfigurationValue(configurationData,
                Path.of(CORE_NAMESPACE, INCLUDED_CLASSES_PATTERNS),
                Collection.class,
                Collections.emptyList());
    }

    /**
     * See {@link HoopoeConfiguration#getExcludedClassesPatterns()}
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getExcludedClassesPatterns() {
        return getConfigurationValue(configurationData,
                Path.of(CORE_NAMESPACE, EXCLUDED_CLASSES_PATTERNS),
                Collection.class,
                Collections.emptyList());
    }

    /**
     * Gets all plugins, enabled in current configuration.
     *
     * @return enabled plugins info or empty collection, if none is enabled.
     */
    public Collection<EnabledComponentData> getEnabledPlugins() {
        return getEnabledComponentsData(PLUGINS_NAMESPACE);
    }

    /**
     * Gets all extensions, enabled in current configuration.
     *
     * @return enabled extensions info or empty collection, if none is enabled.
     */
    public Collection<EnabledComponentData> getEnabledExtensions() {
        return getEnabledComponentsData(EXTENSIONS_NAMESPACE);
    }

    /**
     * Updates {@code pluginConfiguration} with values of plugins configuration section for {@code pluginId}, for all
     * the {@code properties}. Does not set {@code null} values.
     *
     * @param pluginId            ID of plugins to lookup values by.
     * @param pluginConfiguration configuration object to update.
     * @param properties          properties to lookup value for, corresponding setters will be used to update the
     *                            {@code pluginConfiguration}
     */
    public void updatePluginConfiguration(
            String pluginId,
            Object pluginConfiguration,
            Collection<ConfigurationBeanProperty> properties) {

        updateComponentConfiguration(PLUGINS_NAMESPACE, pluginId, pluginConfiguration, properties);
    }

    /**
     * Updates {@code extensionConfiguration} with values of extensions configuration section for {@code extensionId},
     * for all the {@code properties}. Does not set {@code null} values.
     *
     * @param extensionId            ID of extension to lookup values by.
     * @param extensionConfiguration configuration object to update.
     * @param properties             properties to lookup value for, corresponding setters will be used to update the
     *                               {@code extensionConfiguration}
     */
    public void updateExtensionConfiguration(
            String extensionId,
            Object extensionConfiguration,
            Collection<ConfigurationBeanProperty> properties) {

        updateComponentConfiguration(EXTENSIONS_NAMESPACE, extensionId, extensionConfiguration, properties);
    }

    private void updateComponentConfiguration(
            String componentsNamespace,
            String componentId,
            Object pluginConfiguration,
            Collection<ConfigurationBeanProperty> properties) {

        try {
            for (ConfigurationBeanProperty<?> property : properties) {
                Object propertyValue = getConfigurationValue(configurationData,
                        Path.of(componentsNamespace, componentId, property.getKey()),
                        property.getValueType(),
                        null);

                if (propertyValue != null) {
                    property.getSetter().invoke(pluginConfiguration, propertyValue);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new HoopoeException("Error while updating component configuration", e);
        }
    }

    private Collection<EnabledComponentData> getEnabledComponentsData(String componentsNamespace) {
        @SuppressWarnings("unchecked")
        Collection<String> componentsIds = getConfigurationValue(
                configurationData,
                Path.of(componentsNamespace),
                Map.class,
                new HashMap<String, Object>())
                .keySet();

        return componentsIds.stream()
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
     * <p>
     * If value is {@code null} or node is missing in the middle of the path, returns {@code defaultValue}.
     * <p>
     * If any of the intermediate nodes is of any other type but map, fails.
     * <p>
     * If {@code expectedValueType} is not assignable from actual value type, fails.
     *
     * @param path              dot-separated path to traverse by.
     * @param expectedValueType type of the value to be expected and validated.
     * @param defaultValue      value to be returned if path does not have the value.
     * @param <T>               type of the value.
     *
     * @return configuration value, default value or throws an exception.
     */
    @SuppressWarnings("unchecked")
    private <T> T getConfigurationValue(
            Map<String, Object> root,
            Path path,
            Class<T> expectedValueType,
            T defaultValue) {

        Object value = readPathValue(root, path);

        if (value == null) {
            return defaultValue;

        } else {
            Class<?> actualValueType = value.getClass();
            if (!expectedValueType.isAssignableFrom(actualValueType)) {
                throw new HoopoeException("Expected type " + expectedValueType + " does not match actual type " +
                        actualValueType + " for value of " + path);
            }
            return (T) value;
        }
    }

    @SuppressWarnings("unchecked")
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
            path.pathElements = Stream.of(pathElements)
                    .flatMap(pathElement -> Stream.of(pathElement.split("\\.")))
                    .toArray(String[]::new);
            return path;
        }

        @Override
        public String toString() {
            return Stream.of(pathElements).collect(Collectors.joining("."));
        }
    }

}
