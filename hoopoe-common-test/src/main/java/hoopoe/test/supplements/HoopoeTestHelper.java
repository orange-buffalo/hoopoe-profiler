package hoopoe.test.supplements;

import hoopoe.api.HoopoeProfiledInvocation;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

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

    public static Map<String, HoopoeProfiledInvocation> getProfiledInvocationsWithAgentLoaded(TestCode codeToRun)
            throws Exception {

        Map<String, HoopoeProfiledInvocation> capturedData = new HashMap<>();
        doAnswer(invocation ->
                capturedData.put(
                        ((Thread) invocation.getArguments()[0]).getName(),
                        (HoopoeProfiledInvocation) invocation.getArguments()[1]))
                .when(TestConfiguration.getStorageMock())
                .addInvocation(any(), any());

        executeWithAgentLoaded(codeToRun);

        return capturedData;
    }

    public static HoopoeProfiledInvocation getSingleProfiledInvocationWithAgentLoaded(TestCode codeToRun)
            throws Exception {
        Map<String, HoopoeProfiledInvocation> profiledInvocations = getProfiledInvocationsWithAgentLoaded(codeToRun);
        assertThat(profiledInvocations.size(), equalTo(1));
        return profiledInvocations.values().iterator().next();
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
