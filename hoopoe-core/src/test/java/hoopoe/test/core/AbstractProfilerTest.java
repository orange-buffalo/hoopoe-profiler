package hoopoe.test.core;

import hoopoe.test.core.supplements.HoopoeTestConfiguration;
import hoopoe.test.core.supplements.ProfilerTestItem;
import org.junit.Before;

public abstract class AbstractProfilerTest {

    @Before
    public void prepareTest() {
        HoopoeTestConfiguration.resetMocks();
    }

    protected static Object[][] transform(ProfilerTestItem... items) {
        Object[][] data = new Object[items.length][1];
        for (int i = 0; i < items.length; i++) {
            data[i][0] = items[i];
        }
        return data;
    }

}
