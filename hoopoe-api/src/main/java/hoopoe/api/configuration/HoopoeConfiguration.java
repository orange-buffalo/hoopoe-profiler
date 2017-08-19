package hoopoe.api.configuration;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Provides access to the current configuration of the profiler.
 * <p>
 * Allows to read core configuration values and enabled components and their configuration values. Changing of
 * configuration values is not supported.
 */
public interface HoopoeConfiguration {

    /**
     * Method invocations which take less time that this value will not be tracked,
     * i.e. will not be recorded and provided in {@link hoopoe.api.HoopoeProfiledResult}.
     * @return minimum invocation time to track, in nanoseconds.
     */
    long getMinimumTrackedInvocationTimeInNs();

    //TODO add info about plugins and extension

    // todo include/exclude should be merged to "matches" method

    Collection<Pattern> getIncludedClassesPatterns();

    Collection<Pattern> getExcludedClassesPatterns();
}
