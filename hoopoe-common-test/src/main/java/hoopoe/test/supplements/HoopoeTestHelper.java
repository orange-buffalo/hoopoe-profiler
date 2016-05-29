package hoopoe.test.supplements;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HoopoeTestHelper {

    public static long msToNs(long ms) {
        return ms * 1_000_000;
    }

    public static void executeWithAgentLoaded(TestCode codeToRun) throws Exception {
        try {
            TestAgent.load("hoopoe.configuration.class=" + TestConfiguration.class.getCanonicalName());
            codeToRun.execute();
        }
        finally {
            TestAgent.unload();
        }
    }

    public static Object[][] transform(TestItem... items) {
        Object[][] data = new Object[items.length][1];
        for (int i = 0; i < items.length; i++) {
            data[i][0] = items[i];
        }
        return data;
    }

    public interface TestCode {
        void execute() throws Exception;
    }

}
