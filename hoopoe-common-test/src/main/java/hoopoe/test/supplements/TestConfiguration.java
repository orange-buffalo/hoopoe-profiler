package hoopoe.test.supplements;

import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfilerStorage;
import hoopoe.api.HoopoeTracer;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestConfiguration implements HoopoeConfiguration {

    @Mock
    @Getter
    private static HoopoeProfilerStorage storageMock;

    @Mock
    @Getter
    private static HoopoePluginsProvider pluginsProviderMock;

    @Mock
    @Getter
    private static HoopoeTracer tracerMock;

    @Override
    public HoopoeProfilerStorage createProfilerStorage() {
        return storageMock;
    }

    @Override
    public HoopoePluginsProvider createPluginsProvider() {
        return pluginsProviderMock;
    }

    @Override
    public HoopoeTracer createTracer() {
        return tracerMock;
    }

    @Override
    public long getMinimumTrackedInvocationTimeInNs() {
        return 0;   // todo do we need mock here?
    }

    @Override
    public Collection<String> getEnabledPlugins() {
        return Collections.emptyList();
    }

    public static void resetMocks() {
        MockitoAnnotations.initMocks(new TestConfiguration());
    }

}
