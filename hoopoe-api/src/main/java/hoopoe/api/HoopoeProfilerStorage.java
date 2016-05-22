package hoopoe.api;

import java.util.Collection;

public interface HoopoeProfilerStorage {

    void consumeThreadTraceResults(Thread thread, HoopoeTraceNode traceRoot);

    Collection<HoopoeProfiledInvocationSummary> getProfiledInvocationSummaries();

    HoopoeProfiledInvocation getProfiledInvocation(String id);

}
