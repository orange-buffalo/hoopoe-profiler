package hoopoe.api.plugins;

/**
 * Plugins are used to process methods invocations and provide additional information to be linked to this invocation.
 * <p>
 * Typical use-cases would be recording SQL or JPQL queries, recording transactional boundaries etc.
 * <p>
 * Every plugin is loaded in its own classloader which does not follow the Java Classloader Delegation Model. Instead,
 * this classloader first tries to load the class from plugin assembly, and only if the class is missing,
 * delegates to parent classloader. This prevents libraries versions collisions and system classloader pollution.
 * <p>
 * There are some requirements to plugin assembly. See Hoopoe Gradle Plugin / Hoopoe Maven Plugin for details.
 */
public interface HoopoePlugin {

    HoopoeInvocationRecorder createActionIfSupported(HoopoeMethodInfo methodInfo);
}
