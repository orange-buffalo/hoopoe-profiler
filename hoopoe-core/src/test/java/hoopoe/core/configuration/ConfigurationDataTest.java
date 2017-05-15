package hoopoe.core.configuration;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ConfigurationDataTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testFallbackToDefaultValue() throws IOException {
        ConfigurationData configurationData = new ConfigurationData(new HashMap<>());

        Integer actualConfigValue = configurationData.getConfigurationValue("test", Integer.class, 2);
        assertThat(actualConfigValue, equalTo(2));
    }

    @Test
    public void testReadValueOfNestedProperties() throws IOException {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "first", ImmutableMap.of(
                        "second", ImmutableMap.of(
                                "third", "my-value"
                        )
                )
        ));

        String actualConfigValue = configurationData
                .getConfigurationValue("first.second.third", String.class, "42");
        assertThat(actualConfigValue, equalTo("my-value"));
    }

    @Test
    public void testExceptionWhenPathContainsCollection() throws IOException {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "first", ImmutableMap.of(
                        "second", Arrays.asList(1, 2, 3)
                )
        ));

        expectedException.expectMessage("Cannot read first.second.third as it contains collection in the path");

        configurationData.getConfigurationValue("first.second.third", String.class, "42");
    }

    @Test
    public void testReadValueOfSubclass() throws IOException {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of("value", 42L));

        Number actualConfigValue = configurationData.getConfigurationValue("value", Number.class, 35);
        assertThat(actualConfigValue, equalTo(42L));
    }

    @Test
    public void testExceptionWhenReadingWrongValueType() throws IOException {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of("value", 42));

        expectedException.expectMessage("Expected type class java.lang.String does not match " +
                "actual type class java.lang.Integer for value");

        configurationData.getConfigurationValue("value", String.class, "42");
    }

    @Test
    public void testExceptionWhenReadingTerminatedPath() throws IOException {
        ConfigurationData configurationData = new ConfigurationData(ImmutableMap.of(
                "first", ImmutableMap.of(
                        "second", "string"
                )
        ));

        expectedException.expectMessage("Cannot read first.second.third as it is terminated on second");

        configurationData.getConfigurationValue("first.second.third", String.class, "42");
    }

}