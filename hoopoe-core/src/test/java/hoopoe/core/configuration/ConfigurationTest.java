package hoopoe.core.configuration;

import hoopoe.api.configuration.HoopoeConfigurableComponent;
import hoopoe.api.extensions.HoopoeProfilerExtension;
import hoopoe.api.plugins.HoopoePlugin;
import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.withSettings;

public class ConfigurationTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ConfigurationDataReader configurationDataReaderMock;

    @Mock
    private ConfigurationBeanPropertiesReader configurationBeanPropertiesReaderMock;

    @InjectMocks
    private Configuration configuration;

    @Test
    public void testGetEnabledPlugins() {
        Collection<EnabledComponentData> enabledPlugins = configuration.getEnabledPlugins();
        assertThat(enabledPlugins, notNullValue());

        verify(configurationDataReaderMock).readConfiguration();
        verifyNoMoreInteractions(configurationDataReaderMock);
    }

    @Test
    public void testGetEnabledExtension() {
        Collection<EnabledComponentData> enabledExtensions = configuration.getEnabledExtensions();
        assertThat(enabledExtensions, notNullValue());

        verify(configurationDataReaderMock).readConfiguration();
        verifyNoMoreInteractions(configurationDataReaderMock);
    }

    @Test
    public void testSetPluginConfigurationForNotConfigurablePlugin() {
        HoopoePlugin plugin = mock(HoopoePlugin.class);

        configuration.setPluginConfiguration(plugin, "id");

        verifyNoMoreInteractions(configurationDataReaderMock);
    }

    @Test
    public void testSetPluginConfigurationForConfigurablePlugin() {
        HoopoePlugin plugin = mock(
                HoopoePlugin.class,
                withSettings().extraInterfaces(HoopoeConfigurableComponent.class));

        configuration.setPluginConfiguration(plugin, "id");

        verify(configurationDataReaderMock).readConfiguration();
        verifyNoMoreInteractions(configurationDataReaderMock);

        verify(configurationBeanPropertiesReaderMock).readProperties(plugin.getClass());
        verifyNoMoreInteractions(configurationBeanPropertiesReaderMock);
    }

    @Test
    public void testSetExtensionConfigurationForNotConfigurablePlugin() {
        HoopoeProfilerExtension extension = mock(HoopoeProfilerExtension.class);

        configuration.setExtensionConfiguration(extension, "id");

        verifyNoMoreInteractions(configurationDataReaderMock);
    }

    @Test
    public void testSetExtensionConfigurationForConfigurableExtension() {
        HoopoeProfilerExtension extension = mock(
                HoopoeProfilerExtension.class,
                withSettings().extraInterfaces(HoopoeConfigurableComponent.class));

        configuration.setExtensionConfiguration(extension, "id");

        verify(configurationDataReaderMock).readConfiguration();
        verifyNoMoreInteractions(configurationDataReaderMock);

        verify(configurationBeanPropertiesReaderMock).readProperties(extension.getClass());
        verifyNoMoreInteractions(configurationBeanPropertiesReaderMock);
    }

}