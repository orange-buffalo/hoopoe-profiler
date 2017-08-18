package hoopoe.api.plugins;

/**
 * Plugins are used to process methods invocations and provide additional information to be linked to this invocation.
 * <p>
 * Typical use-cases would be recording SQL or JPQL queries, recording transactional boundaries etc.
 * <p>
 * Every plugin is loaded in its own classloader which does not follow the Java Classloader Delegation Model. Instead,
 * this classloader first tries to load the class from plugin assembly, and only if the class is missing, delegates to
 * parent classloader. This prevents libraries versions collisions and system classloader pollution.
 * <p>
 * There are some requirements to plugin assembly. See Hoopoe Gradle Plugin / Hoopoe Maven Plugin for details.
 */
public interface HoopoePlugin {

    /**
     * Is called by profiler core components when new method is instrumented. Plugin responsibility is to check if
     * method is supported by this plugin and return implementation of {@link HoopoeInvocationRecorder} for this method.
     * Recorder will be called whenever application code executes the method described by {@code methodInfo}.
     *
     * @param methodInfo description of the method being instrumented.
     *
     * @return recorder to be called when application code executes the method; {@code null} if method is not supported
     * by the plugin.
     */
    HoopoeInvocationRecorder createActionIfSupported(HoopoeMethodInfo methodInfo);

}
