package hoopoe.test.core;

import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfiler;
import hoopoe.core.HoopoePluginProviderImpl;
import hoopoe.test.core.supplements.HoopoeTestClassLoader;
import hoopoe.utils.HoopoeClassLoader;
import java.util.Collection;
import java.util.Collections;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

public class HoopoePluginProviderImplTest {

    @Rule
    public final ExpectedException exceptionExpectation = ExpectedException.none();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HoopoeProfiler profilerMock;

    private HoopoePluginsProvider pluginProvider;

    @Before
    public void prepareTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader("hoopoe.core");
        Class providerClass = classLoader.loadClass(HoopoePluginProviderImpl.class.getCanonicalName());
        pluginProvider = (HoopoePluginsProvider) providerClass.newInstance();
        pluginProvider.setupProfiler(profilerMock);
    }

    @Test
    public void testMissingZip() {
        when(profilerMock.getConfiguration().getEnabledPlugins())
                .thenReturn(Collections.singleton("this-plugin-is-no-present"));

        exceptionExpectation.expectMessage("this-plugin-is-no-present.zip is not found.");

        pluginProvider.createPlugins();
    }

    @Test
    public void testZipWithoutHoopoeProperties() {
        when(profilerMock.getConfiguration().getEnabledPlugins())
                .thenReturn(Collections.singleton("plugin-hoopoe-properties-missing"));

        exceptionExpectation.expectMessage(
                "plugin-hoopoe-properties-missing.zip does not contain /META-INF/hoopoe.properties file.");

        pluginProvider.createPlugins();
    }

    @Test
    public void testZipWithoutPluginClassProperty() {
        when(profilerMock.getConfiguration().getEnabledPlugins())
                .thenReturn(Collections.singleton("plugin-missing-property"));

        exceptionExpectation.expectMessage(
                "/META-INF/hoopoe.properties in plugin-missing-property.zip does not contain plugin.className property.");

        pluginProvider.createPlugins();
    }

    @Test
    public void testZipWithoutClass() {
        when(profilerMock.getConfiguration().getEnabledPlugins())
                .thenReturn(Collections.singleton("plugin-class-missing"));

        exceptionExpectation.expectMessage(
                "plugin-class-missing.zip does not contain class hoopoe.plugins.TestPlugin.");

        pluginProvider.createPlugins();
    }

    @Test
    public void testValidPlugin() {
        when(profilerMock.getConfiguration().getEnabledPlugins())
                .thenReturn(Collections.singleton("plugin-valid"));

        Collection<HoopoePlugin> actualPlugins = pluginProvider.createPlugins();
        assertThat(actualPlugins, notNullValue());
        assertThat(actualPlugins.size(), equalTo(1));

        HoopoePlugin actualPlugin = actualPlugins.iterator().next();
        assertThat(actualPlugin.getClass().getClassLoader().getClass().getCanonicalName(),
                equalTo(HoopoeClassLoader.class.getCanonicalName()));
    }

}
