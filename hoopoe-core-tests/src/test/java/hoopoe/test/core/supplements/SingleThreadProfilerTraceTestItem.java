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
    public void assertProfiledResult(String originalThreadName,
                                     Map<String, HoopoeProfiledInvocation> profiledResult) {
        assertThat(profiledResult.size(), equalTo(1));
        Map.Entry<String, HoopoeProfiledInvocation> dataEntry = profiledResult.entrySet().iterator().next();
        assertThat(dataEntry.getKey(), equalTo(originalThreadName));
        assertCapturedInvocation(dataEntry.getValue());
    }

    protected abstract void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations);
}