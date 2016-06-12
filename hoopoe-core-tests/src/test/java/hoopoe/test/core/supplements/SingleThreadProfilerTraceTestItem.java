package hoopoe.test.core.supplements;

import hoopoe.api.HoopoeProfiledInvocation;
import java.util.Map;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class SingleThreadProfilerTraceTestItem extends ProfilerTraceTestItem {

    public SingleThreadProfilerTraceTestItem(String description) {
        super(description);
    }

    @Override
    public void assertCapturedData(String originalThreadName,
                                   Map<String, HoopoeProfiledInvocation> capturedData) {
        assertThat(capturedData.size(), equalTo(1));
        Map.Entry<String, HoopoeProfiledInvocation> dataEntry = capturedData.entrySet().iterator().next();
        assertThat(dataEntry.getKey(), equalTo(originalThreadName));
        assertCapturedInvocation(dataEntry.getValue());
    }

    protected abstract void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations);
}