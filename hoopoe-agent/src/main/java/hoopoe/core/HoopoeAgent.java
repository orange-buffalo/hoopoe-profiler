package hoopoe.core;

import hoopoe.utils.HoopoeClassLoader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

/**
 * Java Agent which launches Hoopoe Profiler with provided instrumentation and arguments.
 * <p>
 * Profiler and all related components (including plugins and extensions) are loaded to a separate classloader to avoid
 * any libraries conflicts.
 */
public class HoopoeAgent {

    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        ClassLoader currentClassLoader = HoopoeAgent.class.getClassLoader();
        HoopoeClassLoader hoopoeClassLoader = HoopoeClassLoader.fromResource("/hoopoe-core.zip", currentClassLoader);
        Class<?> bootstrapperClass = Class.forName("hoopoe.core.HoopoeBootstrapper", true, hoopoeClassLoader);
        Method bootstrapMethod = bootstrapperClass.getMethod("bootstrapHoopoe", String.class, Instrumentation.class);
        bootstrapMethod.invoke(null, args, instrumentation);
    }

}
