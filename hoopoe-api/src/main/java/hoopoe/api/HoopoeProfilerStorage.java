package hoopoe.api;

import java.util.Collection;

public interface HoopoeProfilerStorage {

    void addInvocation(Thread thread, HoopoeProfiledInvocation invocation);

    Collection<HoopoeProfiledInvocationSummary> getProfiledInvocationSummaries();

    HoopoeProfiledInvocation getProfiledInvocation(String id);

}
