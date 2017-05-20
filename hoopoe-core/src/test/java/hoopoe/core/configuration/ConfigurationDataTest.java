package hoopoe.core.configuration;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ConfigurationDataTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testExceptionWhenReadingWrongValueType() throws IOException {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "plugins", ImmutableMap.of(
                        "firstPlugin", ImmutableMap.of(
                                // boolean expected for this field
                                "enabled", 1,
                                "path", "testPath"
                        )
                )
        ));

        expectedException.expectMessage("Expected type class java.lang.Boolean does not match " +
                "actual type class java.lang.Integer for value of plugins.firstPlugin.enabled");

        configurationData.getEnabledPlugins();
    }

    @Test
    public void testExceptionWhenReadingTerminatedPath() throws IOException {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "plugins", ImmutableMap.of(
                        // nested map expected here
                        "firstPlugin", 1
                )
        ));

        expectedException.expectMessage("Cannot read plugins.firstPlugin.enabled as it is terminated on firstPlugin");

        configurationData.getEnabledPlugins();
    }

    @Test
    public void testGetEnabledPluginsWhenConfigurationIsEmpty() {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of());

        Collection<EnabledComponentData> actualEnabledPlugins = configurationData.getEnabledPlugins();

        assertThat(actualEnabledPlugins, notNullValue());
        assertThat(actualEnabledPlugins, empty());
    }

    @Test
    public void testGetEnabledPluginsWhenPluginsSectionIsEmpty() {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "plugins", ImmutableMap.of()
        ));

        Collection<EnabledComponentData> actualEnabledPlugins = configurationData.getEnabledPlugins();

        assertThat(actualEnabledPlugins, notNullValue());
        assertThat(actualEnabledPlugins, empty());
    }

    @Test
    public void testGetEnabledPlugins() {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "plugins", ImmutableMap.of(
                        "firstPlugin", ImmutableMap.of(
                                "enabled", false,
                                "path", "testPath"
                        ),
                        "secondPlugin", ImmutableMap.of(
                                "enabled", true,
                                "path", "secondPluginPath"
                        ),
                        "thirdPlugin", ImmutableMap.of(
                                "enabled", true,
                                "path", "thirdPluginPath"
                        )
                )
        ));

        Collection<EnabledComponentData> actualEnabledPlugins = configurationData.getEnabledPlugins();

        assertThat(actualEnabledPlugins, notNullValue());
        assertThat(actualEnabledPlugins, containsInAnyOrder(
                new EnabledComponentData("secondPlugin", "secondPluginPath"),
                new EnabledComponentData("thirdPlugin", "thirdPluginPath")));
    }

    @Test
    public void testGetEnabledExtensions() {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "extensions", ImmutableMap.of(
                        "firstExtension", ImmutableMap.of(
                                "enabled", false,
                                "path", "no matter"
                        ),
                        "myExtension", ImmutableMap.of(
                                "enabled", true,
                                "path", "nice path"
                        )
                )
        ));

        Collection<EnabledComponentData> actualEnabledPlugins = configurationData.getEnabledExtensions();

        assertThat(actualEnabledPlugins, notNullValue());
        assertThat(actualEnabledPlugins, containsInAnyOrder(new EnabledComponentData("myExtension", "nice path")));
    }

    @Test
    public void testExceptionWhenPathIsMissingForPlugin() {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "plugins", ImmutableMap.of(
                        "firstPlugin", ImmutableMap.of(
                                "enabled", true
                        )
                )
        ));

        expectedException.expectMessage("Plugin firstPlugin has no path defined in configuration");

        configurationData.getEnabledPlugins();
    }

    @Test
    public void testUpdatePluginConfiguration() throws InvocationTargetException, IllegalAccessException {
        Method stringSetterMock = Mockito.mock(Method.class);
        ConfigurationBeanProperty stringProperty = ConfigurationBeanProperty.<String>builder()
                .valueType(String.class)
                .key("string.property")
                .setter(stringSetterMock)
                .build();

        Method emptyPropertySetterMock = Mockito.mock(Method.class);
        ConfigurationBeanProperty emptyProperty = ConfigurationBeanProperty.<Integer>builder()
                .valueType(Integer.class)
                .key("int.property")
                .setter(emptyPropertySetterMock)
                .build();

        Object pluginConfiguration = new Object();

        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "plugins", ImmutableMap.of(
                        "myPlugin", ImmutableMap.of(
                                "string", ImmutableMap.of(
                                        "property", "42"
                                )
                                // no data for empty property
                        )
                )
        ));

        configurationData.updatePluginConfiguration(
                "myPlugin",
                pluginConfiguration,
                Arrays.asList(stringProperty, emptyProperty)
        );

        verify(stringSetterMock).invoke(pluginConfiguration, "42");
        verifyNoMoreInteractions(stringSetterMock);
        verifyNoMoreInteractions(emptyPropertySetterMock);
    }

    @Test
    public void testExceptionInUpdatePluginConfiguration() throws InvocationTargetException, IllegalAccessException {
        expectedException.expectMessage("Error while updating component configuration");

        Method stringSetterMock = Mockito.mock(Method.class);
        when(stringSetterMock.invoke(any(),any())).thenThrow(new IllegalAccessException());

        ConfigurationBeanProperty stringProperty = ConfigurationBeanProperty.<String>builder()
                .valueType(String.class)
                .key("string.property")
                .setter(stringSetterMock)
                .build();

        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "plugins", ImmutableMap.of(
                        "myPlugin", ImmutableMap.of(
                                "string", ImmutableMap.of(
                                        "property", "42"
                                )
                        )
                )
        ));

        configurationData.updatePluginConfiguration(
                "myPlugin",
                new Object(),
                Collections.singletonList(stringProperty)
        );
    }

    @Test
    public void testUpdateExtensionConfiguration() throws InvocationTargetException, IllegalAccessException {
        Method stringSetterMock = Mockito.mock(Method.class);
        ConfigurationBeanProperty stringProperty = ConfigurationBeanProperty.<String>builder()
                .valueType(String.class)
                .key("string.property")
                .setter(stringSetterMock)
                .build();

        Method emptyPropertySetterMock = Mockito.mock(Method.class);
        ConfigurationBeanProperty emptyProperty = ConfigurationBeanProperty.<Integer>builder()
                .valueType(Integer.class)
                .key("int.property")
                .setter(emptyPropertySetterMock)
                .build();

        Object extensionConfiguration = new Object();

        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "extensions", ImmutableMap.of(
                        "myExtension", ImmutableMap.of(
                                "string", ImmutableMap.of(
                                        "property", "42"
                                )
                                // no data for empty property
                        )
                )
        ));

        configurationData.updateExtensionConfiguration(
                "myExtension",
                extensionConfiguration,
                Arrays.asList(stringProperty, emptyProperty)
        );

        verify(stringSetterMock).invoke(extensionConfiguration, "42");
        verifyNoMoreInteractions(stringSetterMock);
        verifyNoMoreInteractions(emptyPropertySetterMock);
    }

}