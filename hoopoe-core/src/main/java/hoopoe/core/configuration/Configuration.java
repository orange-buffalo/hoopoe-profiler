package hoopoe.core.configuration;

import hoopoe.api.configuration.HoopoeConfigurableComponent;
import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.api.extensions.HoopoeProfilerExtension;
import hoopoe.api.plugins.HoopoePlugin;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration of the profile.
 * <p>
 * Provides API for reading configuration values as well as component's configuration.
 * <p>
 * Configuration is a result of merge from default and (optional) custom config files, as well as default and (optional)
 * custom configuration profiles.
 */
@Slf4j(topic = "hoopoe.profiler")
public class Configuration implements HoopoeConfiguration {

    private final ConfigurationDataReader configurationDataReader;
    private final ConfigurationBeanPropertiesReader configurationBeanPropertiesReader;
    private ConfigurationData configurationData;

    public Configuration(
            ConfigurationDataReader configurationDataReader,
            ConfigurationBeanPropertiesReader configurationBeanPropertiesReader) {

        this.configurationDataReader = configurationDataReader;
        this.configurationBeanPropertiesReader = configurationBeanPropertiesReader;
    }

    /**
     * Gets data for plugins enabled in configuration.
     *
     * @return collection of {@link EnabledComponentData} for plugins which are enabled; or empty collection if none is
     * enabled.
     */
    public Collection<EnabledComponentData> getEnabledPlugins() {
        return getConfigurationData().getEnabledPlugins();
    }

    /**
     * Gets data for extensions enabled in configuration.
     *
     * @return collection of {@link EnabledComponentData} for extensions which are enabled; or empty collection if none
     * is enabled.
     */
    public Collection<EnabledComponentData> getEnabledExtensions() {
        return getConfigurationData().getEnabledExtensions();
    }

    /**
     * Updates plugin by reading its configuration. Uses {@link hoopoe.api.configuration.HoopoeConfigurationProperty}
     * annotated properties on plugin class to define the configuration properties to be mapped.
     *
     * @param hoopoePlugin plugin to read configuration for.
     * @param pluginId     ID of the plugin, as provided in {@link EnabledComponentData}.
     */
    public void setPluginConfiguration(
            HoopoePlugin hoopoePlugin,
            String pluginId) {

        if (hoopoePlugin instanceof HoopoeConfigurableComponent) {
            HoopoeConfigurableComponent configurableComponent = (HoopoeConfigurableComponent) hoopoePlugin;
            Object componentConfiguration = configurableComponent.getConfiguration();
            Collection<ConfigurationBeanProperty> configurationProperties = configurationBeanPropertiesReader
                    .readProperties(componentConfiguration.getClass());
            getConfigurationData().updatePluginConfiguration(pluginId, componentConfiguration, configurationProperties);
        }
    }

    /**
     * Updates extension by reading its configuration. Uses {@link hoopoe.api.configuration.HoopoeConfigurationProperty}
     * annotated properties on extension class to define the configuration properties to be mapped.
     *
     * @param hoopoeExtension extension to read configuration for.
     * @param extensionId     ID of the extension, as provided in {@link EnabledComponentData}.
     */
    public void setExtensionConfiguration(
            HoopoeProfilerExtension hoopoeExtension,
            String extensionId) {

        if (hoopoeExtension instanceof HoopoeConfigurableComponent) {
            HoopoeConfigurableComponent configurableComponent = (HoopoeConfigurableComponent) hoopoeExtension;
            Object componentConfiguration = configurableComponent.getConfiguration();
            Collection<ConfigurationBeanProperty> configurationProperties = configurationBeanPropertiesReader
                    .readProperties(componentConfiguration.getClass());
            getConfigurationData().updateExtensionConfiguration(
                    extensionId, componentConfiguration, configurationProperties);
        }
    }

    private ConfigurationData getConfigurationData() {
        if (configurationData == null) {
            configurationData = new ConfigurationData(configurationDataReader.readConfiguration());
        }
        return configurationData;
    }

    @Override
    public long getMinimumTrackedInvocationTimeInNs() {
        return configurationData.getMinimumTrackedInvocationTimeInNs();
    }

    @Override
    public Collection<String> getIncludedClassesPatterns() {
        return configurationData.getIncludedClassesPatterns();
    }

    @Override
    public Collection<String> getExcludedClassesPatterns() {
        return configurationData.getExcludedClassesPatterns();
    }
}
