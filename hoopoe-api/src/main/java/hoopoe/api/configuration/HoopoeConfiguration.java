package hoopoe.api.configuration;

import java.util.Collection;

/**
 * Provides access to the current configuration of the profiler.
 * <p>
 * Allows to read core configuration values and enabled components and their configuration values. Changing of
 * configuration values is not supported.
 */
public interface HoopoeConfiguration {

    /**
     * Method invocations which take less time that this value will not be tracked, i.e. will not be recorded and
     * provided in {@link hoopoe.api.HoopoeProfiledResult}.
     *
     * @return minimum invocation time to track, in nanoseconds.
     */
    long getMinimumTrackedInvocationTimeInNs();

    //TODO add info about plugins and extension

    /**
     * Regexp patterns to test canonical class names of instrumented classes. If matches, class is instrumented by
     * profiler. Has higher priority than {@link #getExcludedClassesPatterns()}, i.e. if class matches include pattern,
     * it is NOT tested against excluded patters.
     * <p>
     * By default, all classes are included but internal Hoopoe classes, internal JDK and JVM classes.
     *
     * @return regexp patterns to test canonical class names against.
     */
    Collection<String> getIncludedClassesPatterns();

    /**
     * Regexp patterns to test canonical class names of instrumented classes. If matches, class is skipped and NOT
     * instrumented by profiler.
     * <p>
     * By default, internal Hoopoe classes, internal JDK and JVM classes are excluded.
     *
     * @return regexp patterns to test canonical class names against.
     */
    Collection<String> getExcludedClassesPatterns();
}
