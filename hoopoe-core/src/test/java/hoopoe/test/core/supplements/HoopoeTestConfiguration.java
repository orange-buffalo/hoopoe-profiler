package hoopoe.test.core.supplements;

import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfilerStorage;
import lombok.Getter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HoopoeTestConfiguration implements HoopoeConfiguration {

    @Mock
    @Getter
    private static HoopoeProfilerStorage storageMock;

    @Mock
    @Getter
    private static HoopoePluginsProvider pluginsProviderMock;

    @Override
    public HoopoeProfilerStorage createProfilerStorage() {
        return storageMock;
    }

    @Override
    public HoopoePluginsProvider createPluginsProvider() {
        return pluginsProviderMock;
    }

    public static void resetMocks() {
        MockitoAnnotations.initMocks(new HoopoeTestConfiguration());
    }

}
