package hoopoe.api.extensions;

import hoopoe.api.HoopoeProfiler;

/**
 * Extensions are used to control the profiler, managing profiling results, managing configuration etc.
 * <p>
 * Extensions do not change the logic of core operations, but only control it.
 * <p>
 * The typical use-cases would be a WEB interface, JMX interface etc.
 * <p>
 * Every extension is loaded in its own classloader which does not follow the Java Classloader Delegation Model.
 * Instead, this classloader first tries to load the class from extension assembly, and only if the class is missing,
 * delegates to parent classloader. This prevents libraries versions collisions and system classloader pollution.
 * <p>
 * There are some requirements to extension assembly. See Hoopoe Gradle Plugin / Hoopoe Maven Plugin for details.
 */
public interface HoopoeProfilerExtension {

    /**
     * Called by profiler when it is time to setup this extension.
     *
     * @param profiler profiler this extension is started for.
     */
    void init(HoopoeProfiler profiler);

}
