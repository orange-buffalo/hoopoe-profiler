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

}
