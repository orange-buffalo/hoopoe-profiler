package hoopoe.test.core;

import hoopoe.api.HoopoeMethodInfo;
import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginAction;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeThreadLocalCache;
import hoopoe.test.core.guineapigs.PluginGuineaPig;
import hoopoe.test.supplements.HoopoeTestExecutor;
import hoopoe.test.supplements.TestConfiguration;
import hoopoe.test.supplements.TestConfigurationRule;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HoopoeProfilerImplPluginIntegrationTest {

    private static final String GUINEAPIGS_PACKAGE = "hoopoe.test.core.guineapigs";

    @Rule
    public TestConfigurationRule configurationRule = new TestConfigurationRule();

    @Test
    public void testPluginProviderInitialization() throws Exception {
        HoopoePluginsProvider pluginsProviderMock = TestConfiguration.getPluginsProviderMock();
        doReturn(Collections.emptyList())
                .when(pluginsProviderMock)
                .createPlugins();

        HoopoeTestExecutor.create()
                .withContext(testClassLoader -> new Object())
                .executeWithAgentLoaded(context -> {
                    // we just test initialization of profiler during agent loading
                });

        verify(pluginsProviderMock).setupProfiler(any());
        verify(pluginsProviderMock).createPlugins();
    }

    @Test
    public void testPluginRequest() throws Exception {
        HoopoePlugin pluginMock = preparePluginMock();

        Collection<HoopoeMethodInfo> actualRequests = new HashSet<>();
        doAnswer(invocation -> {
            actualRequests.add((HoopoeMethodInfo) invocation.getArguments()[0]);
            return null;
        }).when(pluginMock).createActionIfSupported(any());

        Class guineaPigClass = PluginGuineaPig.class;

        HoopoeTestExecutor.create()
                .withPackage(GUINEAPIGS_PACKAGE)
                .withContext(testClassLoader -> testClassLoader.loadClass(guineaPigClass.getCanonicalName()))
                .executeWithAgentLoaded(context -> {
                });

        HashSet<String> superClasses = new HashSet<>(Arrays.asList(
                Object.class.getCanonicalName(), Serializable.class.getCanonicalName())
        );

        List<HoopoeMethodInfo> expectedRequests = Arrays.asList(
                new HoopoeMethodInfo(
                        guineaPigClass.getCanonicalName(),
                        "doStuff()",
                        superClasses),
                new HoopoeMethodInfo(
                        guineaPigClass.getCanonicalName(),
                        "testCache()",
                        superClasses
                ),
                new HoopoeMethodInfo(
                        guineaPigClass.getCanonicalName(),
                        "firstMethodInCacheTest()",
                        superClasses
                ),
                new HoopoeMethodInfo(
                        guineaPigClass.getCanonicalName(),
                        "secondMethodInCacheTest()",
                        superClasses
                ),
                new HoopoeMethodInfo(
                        guineaPigClass.getCanonicalName(),
                        "methodForAttributes(java.lang.Object)",
                        superClasses
                ));

        assertThat("Expected request was not triggered", expectedRequests, everyItem(isIn(actualRequests)));
    }

    @Test
    public void testUnsupportedPlugin() throws Exception {
        HoopoePlugin pluginMock = preparePluginMock();
        when(pluginMock.createActionIfSupported(any())).thenReturn(null);

        Class guineaPigClass = PluginGuineaPig.class;

        HoopoeTestExecutor.forClassInstance(guineaPigClass.getCanonicalName())
                .withPackage(GUINEAPIGS_PACKAGE)
                .executeWithAgentLoaded(context -> {
                });

        // basically no verification, just valid execution
    }

    @Test
    public void testSupportedPlugin() throws Exception {
        HoopoePlugin pluginMock = preparePluginMock();
        HoopoePluginAction pluginActionMock = Mockito.mock(HoopoePluginAction.class);
        when(pluginMock.createActionIfSupported(any())).thenReturn(pluginActionMock);
        when(pluginActionMock.getAttributes(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        Class guineaPigClass = PluginGuineaPig.class;

        Object argument = new Object();
        AtomicReference thisInMethod = new AtomicReference();

        HoopoeTestExecutor.forClassInstance(guineaPigClass.getCanonicalName())
                .withPackage(GUINEAPIGS_PACKAGE)
                .executeWithAgentLoaded(context -> {
                    Object instance = context.getInstance();
                    context.getClazz().getMethod("methodForAttributes", Object.class).invoke(instance, argument);
                    thisInMethod.set(instance);
                });

        HashSet<String> superClasses = new HashSet<>(Arrays.asList(
                Object.class.getCanonicalName(), Serializable.class.getCanonicalName())
        );
        verify(pluginMock, times(1))
                .createActionIfSupported(
                        eq(new HoopoeMethodInfo(
                                guineaPigClass.getCanonicalName(),
                                "methodForAttributes(java.lang.Object)",
                                superClasses
                        )));
        verify(pluginActionMock, times(1))
                .getAttributes(
                        eq(new Object[] {argument}),
                        eq(null),
                        eq(thisInMethod.get()),
                        (HoopoeThreadLocalCache) argThat(notNullValue()));
    }

    @Test
    public void testPluginCache() throws Exception {
        AtomicInteger pluginCalls = new AtomicInteger();
        HoopoePlugin plugin = methodInfo -> {
            if (methodInfo.getMethodSignature().equals("firstMethodInCacheTest()")) {
                return (arguments, returnValue, thisInMethod, cache) -> {
                    cache.set(thisInMethod, "testString");
                    pluginCalls.incrementAndGet();
                    return Collections.emptyList();
                };
            }
            else if (methodInfo.getMethodSignature().equals("secondMethodInCacheTest()")) {
                return (arguments, returnValue, thisInMethod, cache) -> {
                    String actualCachedValue = cache.get(thisInMethod);
                    assertThat(actualCachedValue, equalTo("testString"));
                    pluginCalls.incrementAndGet();
                    return Collections.emptyList();
                };
            }
            return null;
        };

        HoopoePluginsProvider pluginsProviderMock = TestConfiguration.getPluginsProviderMock();
        doReturn(Collections.singleton(plugin))
                .when(pluginsProviderMock)
                .createPlugins();

        // todo make this class field
        Class guineaPigClass = PluginGuineaPig.class;

        HoopoeTestExecutor.forClassInstance(guineaPigClass.getCanonicalName())
                .withPackage(GUINEAPIGS_PACKAGE)
                .executeWithAgentLoaded(context -> {
                    Object instance = context.getInstance();
                    context.getClazz().getMethod("testCache").invoke(instance);
                });

        assertThat(pluginCalls.get(), equalTo(2));
    }

    private HoopoePlugin preparePluginMock() {
        HoopoePlugin pluginMock = Mockito.mock(HoopoePlugin.class);

        HoopoePluginsProvider pluginsProviderMock = TestConfiguration.getPluginsProviderMock();
        doReturn(Collections.singleton(pluginMock))
                .when(pluginsProviderMock)
                .createPlugins();

        return pluginMock;
    }

}