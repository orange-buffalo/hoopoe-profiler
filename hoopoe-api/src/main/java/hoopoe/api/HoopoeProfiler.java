package hoopoe.api;

import hoopoe.api.configuration.HoopoeConfiguration;

/**
 * API to work with profiler.
 * Provides methods to start and stop profiling, get the results, get the configuration etc.
 */
public interface HoopoeProfiler {

    void startProfiling();

    HoopoeProfiledResult stopProfiling();

    HoopoeProfiledResult getLastProfiledResult();

    boolean isProfiling();

    HoopoeConfiguration getConfiguration();

    /**
     * Re-calculates hot spots for the last profiled result.
     * <p>
     * Does not cache the calculated result, thus calling this method often could
     * hit application performance.
     *
     * @param hotSpotsCountPerRoot number of hot spots per invocation root. The most time-consuming
     *                             invocations will be selected.
     *
     * @return calculated hot spots. The tree is reverted, from the hot spot to the
     * thread root.
     */
    HoopoeProfiledResult calculateHotSpots(int hotSpotsCountPerRoot);
}
