package hoopoe.test.core.supplements;

import hoopoe.api.HoopoeTraceNode;
import java.util.Map;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class SingleThreadProfilerTraceTestItem extends ProfilerTraceTestItem {

    public SingleThreadProfilerTraceTestItem(String description) {
        super(description);
    }

    @Override
    public void assertCapturedData(String originalThreadName, Map<String, HoopoeTraceNode> capturedData) {
        assertThat(capturedData.size(), equalTo(1));
        Map.Entry<String, HoopoeTraceNode> dataEntry = capturedData.entrySet().iterator().next();
        assertThat(dataEntry.getKey(), equalTo(originalThreadName));
        assertCapturedTraceNode(dataEntry.getValue());
    }

    protected abstract void assertCapturedTraceNode(HoopoeTraceNode traceNode);
}