package hoopoe.test.supplements;

import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfilerStorage;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import lombok.Setter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestConfiguration implements HoopoeConfiguration {

    @Mock
    @Getter
    private static HoopoeProfilerStorage storageMock;

    @Mock
    @Getter
    private static HoopoePluginsProvider pluginsProviderMock;

    @Getter
    @Setter
    private static long minimumTrackedInvocationTimeInNs;

    @Override
    public HoopoeProfilerStorage createProfilerStorage() {
        return storageMock;
    }

    @Override
    public HoopoePluginsProvider createPluginsProvider() {
        return pluginsProviderMock;
    }

    @Override
    public long getMinimumTrackedInvocationTimeInNs() {
        return minimumTrackedInvocationTimeInNs;
    }

    @Override
    public Collection<String> getEnabledPlugins() {
        return Collections.emptyList();
    }

    public static void resetMocks() {
        MockitoAnnotations.initMocks(new TestConfiguration());
        minimumTrackedInvocationTimeInNs = 0;
    }

}
