package hoopoe.test.core.supplements;

import hoopoe.test.core.ProfilerTracingTest;
import java.util.List;
import java.util.Map;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class SingleThreadProfilerTraceTestItem extends ProfilerTraceTestItem {

    public SingleThreadProfilerTraceTestItem(String description) {
        super(description);
    }

    @Override
    public void assertCapturedData(String originalThreadName,
                                   Map<String, List<ProfilerTracingTest.CapturedInvocation>> capturedData) {
        assertThat(capturedData.size(), equalTo(1));
        Map.Entry<String, List<ProfilerTracingTest.CapturedInvocation>> dataEntry = capturedData.entrySet().iterator().next();
        assertThat(dataEntry.getKey(), equalTo(originalThreadName));
        assertCapturedInvocation(dataEntry.getValue());
    }

    protected abstract void assertCapturedInvocation(List<ProfilerTracingTest.CapturedInvocation> invocations);
}