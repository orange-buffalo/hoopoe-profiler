package hoopoe.api;

public interface HoopoeProfilerStorage {

    void consumeThreadTraceResults(Thread thread, HoopoeTraceNode traceRoot);

}
