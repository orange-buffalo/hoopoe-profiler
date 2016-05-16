package hoopoe.test.core.supplements;

import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoeProfilerStorage;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class HoopoeTestConfiguration implements HoopoeConfiguration {

    private static HoopoeProfilerStorage storageMock;

    public HoopoeTestConfiguration() {
        MockitoAnnotations.initMocks(this);
    }

    @Override
    public HoopoeProfilerStorage createProfilerStorage() {
        return storageMock;
    }

    public static HoopoeProfilerStorage createStorageMock() {
        storageMock = Mockito.mock(HoopoeProfilerStorage.class);
        return storageMock;
    }
}
