package hoopoe.test.core;

import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginAction;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.test.core.guineapigs.PluginGuineaPig;
import hoopoe.test.core.supplements.HoopoeTestClassLoader;
import hoopoe.test.core.supplements.HoopoeTestConfiguration;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.containsInAnyOrder;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProfilerPluginIntegrationTest extends AbstractProfilerTest {

    private static final String GUINEAPIGS_PACKAGE = "hoopoe.test.core.guineapigs";

    @Override
    public void prepareTest() {
        super.prepareTest();
    }

    @Test
    public void testPluginProviderInitialization() throws Exception {
        HoopoePluginsProvider pluginsProviderMock = HoopoeTestConfiguration.getPluginsProviderMock();
        doReturn(Collections.emptyList())
                .when(pluginsProviderMock)
                .createPlugins();

        executeWithAgentLoaded(() -> {
            // we just test initialization of profiler during agent loading
        });

        verify(pluginsProviderMock).setupProfiler(any());
        verify(pluginsProviderMock).createPlugins();
    }

    @Test
    public void testPluginRequest() throws Exception {
        HoopoePlugin pluginMock = preparePluginMock();
        when(pluginMock.createActionIfSupported(any(), any(), any())).thenReturn(null);

        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader(GUINEAPIGS_PACKAGE);
        Class guineaPigClass = PluginGuineaPig.class;

        executeWithAgentLoaded(() -> classLoader.loadClass(guineaPigClass.getCanonicalName()));

        Matcher<Iterable<? extends String>> superClassesMatcher = containsInAnyOrder(
                Object.class.getCanonicalName(), Serializable.class.getCanonicalName());

        verify(pluginMock, times(1)).createActionIfSupported(
                eq(guineaPigClass.getCanonicalName()),
                (Collection<String>) argThat(superClassesMatcher),
                eq("PluginGuineaPig(java.lang.Object)"));

        verify(pluginMock, times(1)).createActionIfSupported(
                eq(guineaPigClass.getCanonicalName()),
                (Collection<String>) argThat(superClassesMatcher),
                eq("doStuff()"));

        verifyNoMoreInteractions(pluginMock);
    }

    @Test
    public void testUnsupportedPlugin() throws Exception {
        HoopoePlugin pluginMock = preparePluginMock();
        when(pluginMock.createActionIfSupported(any(), any(), any())).thenReturn(null);

        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader(GUINEAPIGS_PACKAGE);
        Class guineaPigClass = PluginGuineaPig.class;

        executeWithAgentLoaded(() -> {
            Class<?> instrumentedClass = classLoader.loadClass(guineaPigClass.getCanonicalName());
            instrumentedClass.getConstructors()[0].newInstance((Object) null);
        });

        // basically no verification, just valid execution
    }

    @Test
    public void testSupportedPlugin() throws Exception {
        HoopoePlugin pluginMock = preparePluginMock();
        HoopoePluginAction pluginActionMock = Mockito.mock(HoopoePluginAction.class);
        when(pluginMock.createActionIfSupported(any(), any(), any())).thenReturn(pluginActionMock);
        when(pluginActionMock.getAttributes(any(), any())).thenReturn(Collections.emptyList());

        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader(GUINEAPIGS_PACKAGE);
        Class guineaPigClass = PluginGuineaPig.class;

        Object argument = new Object();
        executeWithAgentLoaded(() -> {
            Class<?> instrumentedClass = classLoader.loadClass(guineaPigClass.getCanonicalName());
            instrumentedClass.getConstructors()[0].newInstance(argument);
        });

        Matcher superClassesMatcher = containsInAnyOrder(
                Object.class.getCanonicalName(), Serializable.class.getCanonicalName());
        verify(pluginMock, times(1))
                .createActionIfSupported(
                        eq(guineaPigClass.getCanonicalName()),
                        (Collection<String>) argThat(superClassesMatcher),
                        eq("PluginGuineaPig(java.lang.Object)")
                );
        verify(pluginActionMock, times(1))
                .getAttributes(
                        eq(new Object[] {argument}),
                        eq(null)
                );
    }

    private HoopoePlugin preparePluginMock() {
        HoopoePlugin pluginMock = Mockito.mock(HoopoePlugin.class);

        HoopoePluginsProvider pluginsProviderMock = HoopoeTestConfiguration.getPluginsProviderMock();
        doReturn(Collections.singleton(pluginMock))
                .when(pluginsProviderMock)
                .createPlugins();

        return pluginMock;
    }

}