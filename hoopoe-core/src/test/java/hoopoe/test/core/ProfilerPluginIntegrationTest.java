package hoopoe.test.core;

import hoopoe.api.HoopoeHasAttributes;
import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeTracer;
import hoopoe.test.core.guineapigs.PluginGuineaPig;
import hoopoe.test.core.supplements.HoopoeTestClassLoader;
import hoopoe.test.core.supplements.HoopoeTestConfiguration;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProfilerPluginIntegrationTest extends AbstractProfilerTest {

    @Override
    public void prepareTest() {
        super.prepareTest();
        HoopoeTracer tracerMock = HoopoeTestConfiguration.getTracerMock();
        when(tracerMock.onMethodEnter(any(), any())).thenReturn(Mockito.mock(HoopoeHasAttributes.class));
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
        when(pluginMock.supports(any(), any(), any())).thenReturn(true);

        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader();
        Class guineaPigClass = PluginGuineaPig.class;

        executeWithAgentLoaded(() -> classLoader.loadClass(guineaPigClass.getCanonicalName()));

        Matcher<Iterable<? extends String>> superClassesMatcher = Matchers.containsInAnyOrder(
                Object.class.getCanonicalName(), Serializable.class.getCanonicalName());

        verify(pluginMock, times(1)).supports(
                eq(guineaPigClass.getCanonicalName()),
                (Collection<String>) argThat(superClassesMatcher),
                eq("PluginGuineaPig(java.lang.Object)"));

        verify(pluginMock, times(1)).supports(
                eq(guineaPigClass.getCanonicalName()),
                (Collection<String>) argThat(superClassesMatcher),
                eq("doStuff()"));

        verify(pluginMock, atLeast(2)).getId();

        verifyNoMoreInteractions(pluginMock);
    }

    @Test
    public void testUnsupportedPlugin() throws Exception {
        HoopoePlugin pluginMock = preparePluginMock();
        when(pluginMock.supports(any(), any(), any())).thenReturn(false);

        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader();
        Class guineaPigClass = PluginGuineaPig.class;

        executeWithAgentLoaded(() -> {
            Class<?> instrumentedClass = classLoader.loadClass(guineaPigClass.getCanonicalName());
            instrumentedClass.getConstructors()[0].newInstance((Object) null);
        });

        verify(pluginMock, never()).getAttributes(any(), any(), any(), any());
    }

    @Test
    public void testSupportedPlugin() throws Exception {
        HoopoePlugin pluginMock = preparePluginMock();
        when(pluginMock.supports(any(), any(), any())).thenReturn(true);
        when(pluginMock.getAttributes(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader();
        Class guineaPigClass = PluginGuineaPig.class;

        Object argument = new Object();
        executeWithAgentLoaded(() -> {
            Class<?> instrumentedClass = classLoader.loadClass(guineaPigClass.getCanonicalName());
            instrumentedClass.getConstructors()[0].newInstance(argument);
        });

        Matcher<String[]> superClassesMatcher = Matchers.arrayContainingInAnyOrder(
                Object.class.getCanonicalName(), Serializable.class.getCanonicalName());
        verify(pluginMock, times(1))
                .getAttributes(
                        eq(guineaPigClass.getCanonicalName()),
                        argThat(superClassesMatcher),
                        eq("PluginGuineaPig(java.lang.Object)"),
                        eq(new Object[] {argument})
                );
    }

    private HoopoePlugin preparePluginMock() {
        HoopoePlugin pluginMock = Mockito.mock(HoopoePlugin.class);
        when(pluginMock.getId()).thenReturn("id");

        HoopoePluginsProvider pluginsProviderMock = HoopoeTestConfiguration.getPluginsProviderMock();
        doReturn(Collections.singleton(pluginMock))
                .when(pluginsProviderMock)
                .createPlugins();

        return pluginMock;
    }

}