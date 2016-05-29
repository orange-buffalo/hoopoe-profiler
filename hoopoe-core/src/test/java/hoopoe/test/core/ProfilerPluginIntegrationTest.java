package hoopoe.test.core;

import hoopoe.api.HoopoeMethodInfo;
import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginAction;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeThreadLocalCache;
import hoopoe.test.core.guineapigs.PluginGuineaPig;
import hoopoe.test.core.supplements.HoopoeTestClassLoader;
import hoopoe.test.core.supplements.HoopoeTestConfiguration;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import static org.hamcrest.CoreMatchers.notNullValue;
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
        when(pluginMock.createActionIfSupported(any())).thenReturn(null);

        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader(GUINEAPIGS_PACKAGE);
        Class guineaPigClass = PluginGuineaPig.class;

        executeWithAgentLoaded(() -> classLoader.loadClass(guineaPigClass.getCanonicalName()));

        HashSet<String> superClasses = new HashSet<>(Arrays.asList(
                Object.class.getCanonicalName(), Serializable.class.getCanonicalName())
        );

        verify(pluginMock, times(1)).createActionIfSupported(
                eq(new HoopoeMethodInfo(
                        guineaPigClass.getCanonicalName(),
                        "PluginGuineaPig(java.lang.Object)",
                        superClasses
                )));

        verify(pluginMock, times(1)).createActionIfSupported(
                eq(new HoopoeMethodInfo(
                        guineaPigClass.getCanonicalName(),
                        "doStuff()",
                        superClasses
                )));

        verifyNoMoreInteractions(pluginMock);
    }

    @Test
    public void testUnsupportedPlugin() throws Exception {
        HoopoePlugin pluginMock = preparePluginMock();
        when(pluginMock.createActionIfSupported(any())).thenReturn(null);

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
        when(pluginMock.createActionIfSupported(any())).thenReturn(pluginActionMock);
        when(pluginActionMock.getAttributes(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader(GUINEAPIGS_PACKAGE);
        Class guineaPigClass = PluginGuineaPig.class;

        Object argument = new Object();
        AtomicReference thisInMethod = new AtomicReference();
        executeWithAgentLoaded(() -> {
            Class<?> instrumentedClass = classLoader.loadClass(guineaPigClass.getCanonicalName());
            Object instance = instrumentedClass.getConstructors()[0].newInstance(argument);
            thisInMethod.set(instance);
        });

        HashSet<String> superClasses = new HashSet<>(Arrays.asList(
                Object.class.getCanonicalName(), Serializable.class.getCanonicalName())
        );
        verify(pluginMock, times(1))
                .createActionIfSupported(
                        eq(new HoopoeMethodInfo(
                        guineaPigClass.getCanonicalName(),
                        "PluginGuineaPig(java.lang.Object)",
                        superClasses
                )));
        verify(pluginActionMock, times(1))
                .getAttributes(
                        eq(new Object[] {argument}),
                        eq(null),
                        eq(thisInMethod.get()),
                        (HoopoeThreadLocalCache) argThat(notNullValue()));
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