package hoopoe.core.configuration;

import hoopoe.api.configuration.HoopoeConfigurableComponent;
import hoopoe.api.extensions.HoopoeProfilerExtension;
import hoopoe.api.plugins.HoopoePlugin;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "hoopoe.profiler")
public class Configuration {

    private final ConfigurationDataReader configurationDataReader;
    private final ConfigurationBeanPropertiesReader configurationBeanPropertiesReader;
    private ConfigurationData configurationData;

    public Configuration(
            ConfigurationDataReader configurationDataReader,
            ConfigurationBeanPropertiesReader configurationBeanPropertiesReader) {

        this.configurationDataReader = configurationDataReader;
        this.configurationBeanPropertiesReader = configurationBeanPropertiesReader;
    }

    public Collection<EnabledComponentData> getEnabledPlugins() {
        return getConfigurationData().getEnabledPlugins();
    }

    public Collection<EnabledComponentData> getEnabledExtensions() {
        return getConfigurationData().getEnabledExtensions();
    }

    public void setPluginConfiguration(
            HoopoePlugin hoopoePlugin,
            String pluginId) {

        if (hoopoePlugin instanceof HoopoeConfigurableComponent) {
            HoopoeConfigurableComponent configurableComponent = (HoopoeConfigurableComponent) hoopoePlugin;
            Collection<ConfigurationBeanProperty> configurationProperties = configurationBeanPropertiesReader
                    .readProperties(configurableComponent.getClass());
            getConfigurationData().updatePluginConfiguration(pluginId, configurableComponent, configurationProperties);
        }
    }

    public void setExtensionConfiguration(
            HoopoeProfilerExtension hoopoeExtension,
            String extensionId) {

        if (hoopoeExtension instanceof HoopoeConfigurableComponent) {
            HoopoeConfigurableComponent configurableComponent = (HoopoeConfigurableComponent) hoopoeExtension;
            Collection<ConfigurationBeanProperty> configurationProperties = configurationBeanPropertiesReader
                    .readProperties(configurableComponent.getClass());
            getConfigurationData().updateExtensionConfiguration(
                    extensionId, configurableComponent, configurationProperties);
        }
    }

    private ConfigurationData getConfigurationData() {
        if (configurationData == null) {
            configurationData = new ConfigurationData(configurationDataReader.readConfiguration());
        }
        return configurationData;
    }
}
