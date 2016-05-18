package hoopoe.test.core;

import hoopoe.test.core.supplements.HoopoeTestAgent;
import hoopoe.test.core.supplements.HoopoeTestConfiguration;
import hoopoe.test.core.supplements.ProfilerTraceTestItem;
import org.junit.Before;

public abstract class AbstractProfilerTest {

    @Before
    public void prepareTest() {
        HoopoeTestConfiguration.resetMocks();
    }

    protected void executeWithAgentLoaded(TestCode codeToRun) throws Exception {
        try {
            HoopoeTestAgent.load("hoopoe.configuration.class=" + HoopoeTestConfiguration.class.getCanonicalName());
            codeToRun.execute();
        }
        finally {
            HoopoeTestAgent.unload();
        }
    }

    protected static Object[][] transform(ProfilerTraceTestItem... items) {
        Object[][] data = new Object[items.length][1];
        for (int i = 0; i < items.length; i++) {
            data[i][0] = items[i];
        }
        return data;
    }

    protected interface TestCode {
        void execute() throws Exception;
    }

}
