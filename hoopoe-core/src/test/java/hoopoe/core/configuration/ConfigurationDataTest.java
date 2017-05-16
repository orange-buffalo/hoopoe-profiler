package hoopoe.core.configuration;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;

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

}