package hoopoe.test.core;

import hoopoe.api.HoopoePluginsProvider;
import hoopoe.test.core.supplements.HoopoeTestAgent;
import hoopoe.test.core.supplements.HoopoeTestConfiguration;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mockito;

public class ProfilerPluginIntegrationTest extends AbstractProfilerTest {

    @Test
    public void testPluginProviderInitialization() throws Exception {
        HoopoePluginsProvider pluginsProviderMock = HoopoeTestConfiguration.getPluginsProviderMock();
        Mockito.doReturn(Collections.emptyList())
                .when(pluginsProviderMock)
                .createPlugins();

        try {
            HoopoeTestAgent.load("hoopoe.configuration.class=" + HoopoeTestConfiguration.class.getCanonicalName());

            Mockito.verify(pluginsProviderMock).setupProfiler(Mockito.any());
            Mockito.verify(pluginsProviderMock).createPlugins();
        }
        finally {
            HoopoeTestAgent.unload();
        }
    }

}