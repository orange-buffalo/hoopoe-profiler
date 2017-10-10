package hoopoe.core.components;

import hoopoe.api.plugins.HoopoeInvocationRecorder;
import hoopoe.api.plugins.HoopoeMethodInfo;
import hoopoe.api.plugins.HoopoePlugin;
import hoopoe.core.configuration.Configuration;
import hoopoe.core.configuration.EnabledComponentData;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginsManagerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private Configuration configurationMock;

    @Mock
    private ComponentLoader componentLoaderMock;

    @Mock
    private HoopoeMethodInfo methodInfoMock;

    private EnabledComponentData pluginData = new EnabledComponentData("firstPlugin", null);

    @Mock
    private HoopoePlugin pluginMock;

    @Test
    public void testNoPluginsEnabled() {
        assertThat("RecordersReference must be 0 when no plugin are enabled",
                getPluginsManager().getPluginRecordersReference(methodInfoMock),
                equalTo(0L));
    }

    @Test
    public void testSingleRecorderRegistered() {
        when(configurationMock.getEnabledPlugins()).thenReturn(Collections.singleton(pluginData));
        when(componentLoaderMock.loadComponent(any(), any())).thenReturn(pluginMock);
        when(pluginMock.createRecorderIfSupported(eq(methodInfoMock)))
                .thenReturn(mock(HoopoeInvocationRecorder.class));

        assertThat("RecordersReference must be have first bit enabled for single invocation recorder",
                getPluginsManager().getPluginRecordersReference(methodInfoMock),
                equalTo(1L));
    }

    @Test
    public void testPluginEnabledWithoutRecorder() {
        when(configurationMock.getEnabledPlugins()).thenReturn(Collections.singleton(pluginData));
        when(componentLoaderMock.loadComponent(any(), any())).thenReturn(pluginMock);
        when(pluginMock.createRecorderIfSupported(eq(methodInfoMock))).thenReturn(null);

        assertThat("RecordersReference must be 0 when enabled plugin has not provided a recorder",
                getPluginsManager().getPluginRecordersReference(methodInfoMock),
                equalTo(0L));
    }

    @Test
    public void testMultipleRecordersRegistered() {
        when(configurationMock.getEnabledPlugins()).thenReturn(Arrays.asList(pluginData, pluginData));
        when(componentLoaderMock.loadComponent(any(), any())).thenReturn(pluginMock);
        when(pluginMock.createRecorderIfSupported(eq(methodInfoMock)))
                .thenAnswer((Answer<HoopoeInvocationRecorder>) invocation -> mock(HoopoeInvocationRecorder.class));

        assertThat(
                "RecordersReference must be have two first bits enabled for when two invocation recorders registered",
                getPluginsManager().getPluginRecordersReference(methodInfoMock),
                equalTo(3L));
    }

    @Test
    public void testMaximumNumberOfRecordersRegistered() {
        EnabledComponentData[] pluginsData = new EnabledComponentData[64];
        Arrays.fill(pluginsData, pluginData);
        when(configurationMock.getEnabledPlugins()).thenReturn(Arrays.asList(pluginsData));

        when(componentLoaderMock.loadComponent(any(), any())).thenReturn(pluginMock);

        when(pluginMock.createRecorderIfSupported(eq(methodInfoMock)))
                .thenAnswer((Answer<HoopoeInvocationRecorder>) invocation -> mock(HoopoeInvocationRecorder.class));

        assertThat(
                "When maximum number of recorders is registered, all bits should be up",
                getPluginsManager().getPluginRecordersReference(methodInfoMock),
                equalTo(0xFFFFFFFFFFFFFFFFL));
    }

    @Test
    public void testMaximumNumberOfRecordersExceeded() {
        EnabledComponentData[] pluginsData = new EnabledComponentData[65];
        Arrays.fill(pluginsData, pluginData);
        when(configurationMock.getEnabledPlugins()).thenReturn(Arrays.asList(pluginsData));

        when(componentLoaderMock.loadComponent(any(), any())).thenReturn(pluginMock);

        when(pluginMock.createRecorderIfSupported(eq(methodInfoMock)))
                .thenAnswer((Answer<HoopoeInvocationRecorder>) invocation -> mock(HoopoeInvocationRecorder.class));

        expectedException.expectMessage(
                "Maximum number of 64 recorders is reached. Probably some of plugins are not optimized.");

        getPluginsManager().getPluginRecordersReference(methodInfoMock);
    }

    private PluginsManager getPluginsManager() {
        return new PluginsManager(configurationMock, componentLoaderMock);
    }

}